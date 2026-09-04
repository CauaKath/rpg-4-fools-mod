package net.abakath.rpg4fools.init;


import net.abakath.rpg4fools.server.LoadedChunks;
import net.abakath.rpg4fools.server.events.compost.BarkStripping;
import net.abakath.rpg4fools.server.events.compost.CompostExpiry;
import net.abakath.rpg4fools.server.events.compost.CompostHandling;
import net.abakath.rpg4fools.server.events.compost.CompostSignals;
import net.abakath.rpg4fools.server.events.compost.CompostHarvest;
import net.abakath.rpg4fools.server.events.crop.CropAutoReplant;
import net.abakath.rpg4fools.server.events.crop.CropOwnership;
import net.abakath.rpg4fools.server.events.crop.CropSettling;
import net.abakath.rpg4fools.server.events.trellis.CropStickHandling;
import net.abakath.rpg4fools.server.events.crop.CropTagValidator;
import net.abakath.rpg4fools.server.events.trellis.CropWallHandling;
import net.abakath.rpg4fools.server.events.season.DayChangingHandler;
import net.abakath.rpg4fools.server.events.crop.HoeAreaHarvest;
import net.abakath.rpg4fools.server.events.compost.ManureDropping;
import net.abakath.rpg4fools.server.events.season.SeasonPlantingGate;
import net.abakath.rpg4fools.server.events.season.SnowMeltHandler;
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
    CropSettling.register();
    CompostExpiry.register();
    CompostHarvest.register();
    BarkStripping.register();
  }
}
