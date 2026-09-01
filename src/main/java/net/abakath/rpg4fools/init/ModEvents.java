package net.abakath.rpg4fools.init;


import net.abakath.rpg4fools.server.LoadedChunks;
import net.abakath.rpg4fools.server.events.BarkStripping;
import net.abakath.rpg4fools.server.events.CompostExpiry;
import net.abakath.rpg4fools.server.events.CompostHandling;
import net.abakath.rpg4fools.server.events.CompostHarvest;
import net.abakath.rpg4fools.server.events.CropAutoReplant;
import net.abakath.rpg4fools.server.events.CropOwnership;
import net.abakath.rpg4fools.server.events.CropSettling;
import net.abakath.rpg4fools.server.events.CropStickHandling;
import net.abakath.rpg4fools.server.events.CropTagValidator;
import net.abakath.rpg4fools.server.events.CropWallHandling;
import net.abakath.rpg4fools.server.events.DayChangingHandler;
import net.abakath.rpg4fools.server.events.ManureDropping;
import net.abakath.rpg4fools.server.events.SeasonPlantingGate;
import net.abakath.rpg4fools.server.events.SnowMeltHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModEvents {
  public static void registerEvents() {
    ServerTickEvents.START_SERVER_TICK.register(new DayChangingHandler());
    ServerTickEvents.START_SERVER_TICK.register(new SnowMeltHandler());
    ServerTickEvents.END_SERVER_TICK.register(new ManureDropping());

    LoadedChunks.register();
    CropTagValidator.register();
    SeasonPlantingGate.register();
    CropStickHandling.register();
    CropWallHandling.register();
    CropAutoReplant.register();
    CropOwnership.register();
    CropSettling.register();
    CompostHandling.register();
    CompostExpiry.register();
    CompostHarvest.register();
    BarkStripping.register();
  }
}
