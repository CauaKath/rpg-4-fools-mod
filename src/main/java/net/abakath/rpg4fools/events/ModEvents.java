package net.abakath.rpg4fools.events;


import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModEvents {

  public static void registerEvents() {
    ServerTickEvents.START_SERVER_TICK.register(new TickHandler());
  }

}
