package net.abakath.rpg4fools;

import net.abakath.rpg4fools.client.ClientModMessages;
import net.abakath.rpg4fools.client.SeasonColorProviders;
import net.abakath.rpg4fools.client.SeasonsHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class RPG4FoolsClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    HudRenderCallback.EVENT.register(new SeasonsHudOverlay());

    ClientModMessages.registerS2CReceivers();
    SeasonColorProviders.register();
  }
}
