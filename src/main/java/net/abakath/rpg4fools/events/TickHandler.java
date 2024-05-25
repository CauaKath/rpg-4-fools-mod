package net.abakath.rpg4fools.events;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class TickHandler implements ClientTickEvents.StartTick {

    @Override
    public void onStartTick(MinecraftClient client) {
        if (client.world != null) {
            long time = client.world.getTimeOfDay();
            boolean newDay = time % 24000 == 0;

            if (newDay) {
                int totalDays = (int) (time / 24000);
                double year = Math.ceil((double) totalDays / 360);
                double month = Math.ceil((double) totalDays / 30);
                System.out.println("Day " + totalDays);
                System.out.println("Year " + year);
                System.out.println("Month " + month);
            }
        }
    }
}
