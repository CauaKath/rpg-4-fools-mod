package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.models.DayData;
import net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket;
import net.abakath.rpg4fools.server.SeasonData;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

    @Override
    public void onStartTick(MinecraftServer server) {
        if (server.getOverworld() != null) {
            long time = server.getOverworld().getTimeOfDay();

            if (time <= 0) {
                return;
            }

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

            server.getPlayerManager().getPlayerList().forEach(player -> {
                DayData.setPlayerDayData((IEntityDataSaver) player, dayData);
                ServerPlayNetworking.send(player, new SeasonUpdatePacket(dayData));
            });
        }
    }

    @NotNull
    private static DayData getDayData(long time) {
        int totalDays = (int)  Math.floor(((double) time / DAY_DURATION));
        int year = (int) Math.ceil((double) (totalDays + 1) / YEAR_DURATION);
        int month = (int) Math.floor((double) (totalDays / MONTH_DURATION) - ((year - 1) * MONTHS_IN_YEAR));
        int day = totalDays - ((month * MONTH_DURATION) - 1) - ((year - 1) * YEAR_DURATION);

        return new DayData(year, Months.values()[month], day, time);
    }
}
