package net.abakath.rpg4fools.events;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.models.DayData;
import net.abakath.rpg4fools.networking.packets.SeasonUpdateS2CPacket;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class TickHandler implements ServerTickEvents.StartTick {
    private static final int DAY_DURATION = 24000;
    private static final int MONTH_DURATION = 28;
    private static final int MONTHS_IN_YEAR = 12;
    private static final int YEAR_DURATION = MONTH_DURATION * MONTHS_IN_YEAR;

    @Override
    public void onStartTick(MinecraftServer server) {
        if (server.getOverworld() != null) {
            long time = server.getOverworld().getTimeOfDay();

            if (time <= 0) {
                return;
            }

            DayData dayData = getDayData(time);
            server.getPlayerManager().getPlayerList().forEach(player -> {
                DayData.setPlayerDayData((IEntityDataSaver) player, dayData);
                ServerPlayNetworking.send(player, new SeasonUpdateS2CPacket(dayData));
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
