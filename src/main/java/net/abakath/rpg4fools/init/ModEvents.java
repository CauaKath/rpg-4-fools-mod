package net.abakath.rpg4fools.init;


import net.abakath.rpg4fools.server.LoadedChunks;
import net.abakath.rpg4fools.server.events.BarkStripping;
import net.abakath.rpg4fools.server.events.CompostExpiry;
import net.abakath.rpg4fools.server.events.CompostHandling;
import net.abakath.rpg4fools.server.events.CompostSignals;
import net.abakath.rpg4fools.server.events.CompostHarvest;
import net.abakath.rpg4fools.server.events.CropAutoReplant;
import net.abakath.rpg4fools.server.debug.SnowDebugCommand;
import net.abakath.rpg4fools.server.events.CropOwnership;
import net.abakath.rpg4fools.server.events.CropSettling;
import net.abakath.rpg4fools.server.events.CropStickHandling;
import net.abakath.rpg4fools.server.events.CropTagValidator;
import net.abakath.rpg4fools.server.events.CropWallHandling;
import net.abakath.rpg4fools.server.events.DayChangingHandler;
import net.abakath.rpg4fools.server.events.HoeAreaHarvest;
import net.abakath.rpg4fools.server.events.ManureDropping;
import net.abakath.rpg4fools.server.events.SeasonPlantingGate;
import net.abakath.rpg4fools.server.events.SnowMeltHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModEvents {
  public static void registerEvents() {
    ServerTickEvents.START_SERVER_TICK.register(new DayChangingHandler());
    ServerTickEvents.START_SERVER_TICK.register(new SnowMeltHandler());
    ServerTickEvents.END_SERVER_TICK.register(new ManureDropping());
    ServerTickEvents.END_SERVER_TICK.register(new CompostSignals());

    DayChangingHandler.register();
    LoadedChunks.register();
    CropTagValidator.register();
    SeasonPlantingGate.register();
    CropStickHandling.register();
    CropWallHandling.register();

    // Before the auto replant, which harvests any ripe crop that is right clicked. The compost
    // handler only claims a click when the player is holding compost or a shovel, and without this
    // order a field could never be composted once it ripened.
    CompostHandling.register();

    // Also before the auto replant, and for the same kind of reason: a hoe harvests the square
    // around the crop it clicked, and the auto replant would otherwise take that crop on its own
    // first. Hoes below iron pass straight through to it.
    HoeAreaHarvest.register();
    CropAutoReplant.register();
    CropOwnership.register();
    SnowDebugCommand.register();
    CropSettling.register();
    CompostExpiry.register();
    CompostHarvest.register();
    BarkStripping.register();
  }
}
