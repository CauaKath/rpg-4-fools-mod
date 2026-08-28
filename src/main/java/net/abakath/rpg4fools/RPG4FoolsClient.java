package net.abakath.rpg4fools;

import net.abakath.rpg4fools.client.ClientModMessages;
import net.abakath.rpg4fools.client.SeasonAtmosphere;
import net.abakath.rpg4fools.client.SeasonColorProviders;
import net.abakath.rpg4fools.client.SeasonsHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class RPG4FoolsClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    HudRenderCallback.EVENT.register(new SeasonsHudOverlay());

    ClientModMessages.registerS2CReceivers();
    SeasonColorProviders.register();

    // Biome tags arrive as part of the join handshake. Anything resolved before they land falls
    // back to DEFAULT, and the atmosphere memoises per registry entry, so that wrong answer would
    // stick for the session and leave the world on vanilla fog.
    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SeasonAtmosphere.clearCaches());
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SeasonAtmosphere.clearCaches());
  }
}
