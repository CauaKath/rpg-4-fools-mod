package net.abakath.rpg4fools.init;


import net.abakath.rpg4fools.server.events.DayChangingHandler;
import net.abakath.rpg4fools.server.events.SnowMeltHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModEvents {
  public static void registerEvents() {
    ServerTickEvents.START_SERVER_TICK.register(new DayChangingHandler());
    ServerTickEvents.START_SERVER_TICK.register(new SnowMeltHandler());
  }
}
