package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.models.DayData;
import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.abakath.rpg4fools.server.SeasonData;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class DayChangingHandler implements ServerTickEvents.StartTick {
    private static final int DAY_DURATION = 24000;
    private static final int MONTH_DURATION = 28;
    private static final int MONTHS_IN_YEAR = 12;
    private static final int YEAR_DURATION = MONTH_DURATION * MONTHS_IN_YEAR;

    /**
     * The season as of the last tick, for spotting the moment it turns.
     *
     * <p>Null until the first tick, which deliberately skips the sweep on start up: nothing has
     * changed yet, and a full pass over every loaded chunk is not something to do while a server is
     * still coming up.
     */
    private Season lastSeason = null;

    /**
     * The day the last update went out for. The date only moves once every DAY_DURATION ticks, so
     * everything below is gated on this rather than run every tick.
     */
    private int lastTotalDays = Integer.MIN_VALUE;

    /**
     * The server the two fields above were last filled in for.
     *
     * <p>This handler is registered once at mod init, so in single player it outlives the world. A
     * second world would otherwise inherit the first one's day and season: the day would suppress
     * the update that seeds CurrentSeason, and the season would fire a change sweep on the first
     * tick that the null start above exists to avoid.
     */
    private MinecraftServer lastServer = null;

    /**
     * A joining player has missed whatever went out at the last day change, so it is repeated for
     * them here. This is the only reason the date is ever sent outside a day change.
     */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (server.overworld() == null) {
                return;
            }

            long time = server.overworld().getDayTime();

            if (time <= 0) {
                return;
            }

            ServerPlayNetworking.send(handler.getPlayer(), new SeasonUpdatePacket(getDayData(time)));
        });
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        if (server.overworld() != null) {
            if (server != lastServer) {
                lastServer = server;
                lastTotalDays = Integer.MIN_VALUE;
                lastSeason = null;
            }

            long time = server.overworld().getDayTime();

            if (time <= 0) {
                return;
            }

            int totalDays = totalDays(time);

            if (totalDays == lastTotalDays) {
                return;
            }

            lastTotalDays = totalDays;

            DayData dayData = getDayData(time);
            SeasonData seasonData = SeasonData.getServerState(server);

            seasonData.setSubSeason(dayData.getMonth().getSubSeason());

            // The precipitation mixin reads this on both sides. The server half is set here rather
            // than read back from SeasonData, so the hot path never touches persistent state.
            CurrentSeason.set(dayData.getMonth().getSubSeason());

            Season season = dayData.getMonth().getSubSeason().getSeason();
            if (lastSeason != null && lastSeason != season) {
                SeasonChangeSweep.run(server, season);
            }
            lastSeason = season;

            server.getPlayerList().getPlayers().forEach(player ->
                    ServerPlayNetworking.send(player, new SeasonUpdatePacket(dayData)));
        }
    }

    private static int totalDays(long time) {
        return (int) Math.floor(((double) time / DAY_DURATION));
    }

    @NotNull
    private static DayData getDayData(long time) {
        int totalDays = totalDays(time);
        int year = (int) Math.ceil((double) (totalDays + 1) / YEAR_DURATION);
        int month = (int) Math.floor((double) (totalDays / MONTH_DURATION) - ((year - 1) * MONTHS_IN_YEAR));
        int day = totalDays - ((month * MONTH_DURATION) - 1) - ((year - 1) * YEAR_DURATION);

        return new DayData(year, Months.values()[month], day, time);
    }
}
