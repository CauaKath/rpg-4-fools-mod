package net.abakath.rpg4fools.events;


import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ModEvents {

  public static void registerEvents() {
    // register tick handler
    ClientTickEvents.START_CLIENT_TICK.register(new TickHandler());
  }

}
