package net.abakath.rpg4fools;

import net.abakath.rpg4fools.client.ClientModMessages;
import net.abakath.rpg4fools.client.season.CropSeasonTooltip;
import net.abakath.rpg4fools.client.atmosphere.SeasonAtmosphere;
import net.abakath.rpg4fools.client.season.SeasonColorProviders;
import net.abakath.rpg4fools.client.season.SeasonsHudOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.resources.Identifier;

public class RPG4FoolsClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "seasons_overlay"),
            new SeasonsHudOverlay());

    ClientModMessages.registerS2CReceivers();
    SeasonColorProviders.register();
    CropSeasonTooltip.register();

    // Biome tags arrive as part of the join handshake. Anything resolved before they land falls
    // back to DEFAULT, and the atmosphere memoises per registry entry, so that wrong answer would
    // stick for the session and leave the world on vanilla fog.
    //
    // JOIN fires before the tags do, which is why TAGS_LOADED is here as well: clearing on join
    // alone left the aggregate resolved at the spawn point cached without any fog, and it stayed
    // that way until the player walked far enough to land in a different cache cell.
    CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> SeasonAtmosphere.clearCaches());
    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SeasonAtmosphere.clearCaches());
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SeasonAtmosphere.clearCaches());
  }
}
