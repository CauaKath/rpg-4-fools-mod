package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports crops that were never given a season.
 *
 * <p>The rule is that a crop carries one or more season tags, but enforcing it by refusing to load
 * would punish the wrong person: a datapack or another mod can add to #rpg4fools:crops without ever
 * touching the season tags. CropSeasons already falls back to all-season for those, so the only
 * thing missing is a way to notice. This is that.
 *
 * <p>Runs on datapack reload as well as server start, since /reload is where tags actually change.
 */
public class CropTagValidator {
  public static void register() {
    ServerLifecycleEvents.SERVER_STARTED.register(CropTagValidator::validate);

    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
      if (success) {
        validate(server);
      }
    });
  }

  private static void validate(MinecraftServer server) {
    Registry<Block> blocks = server.getRegistryManager().get(RegistryKeys.BLOCK);

    blocks.getEntryList(ModBlockTags.CROPS).ifPresent(crops -> {
      List<String> seasonless = new ArrayList<>();

      for (RegistryEntry<Block> crop : crops) {
        if (hasNoSeason(crop)) {
          seasonless.add(crop.getKey().map(key -> key.getValue().toString()).orElse("unregistered block"));
        }
      }

      if (!seasonless.isEmpty()) {
        RPG4Fools.LOGGER.warn(
                "{} crop(s) in #{} carry no grows_in_* tag and will be treated as growing in every season: {}",
                seasonless.size(),
                ModBlockTags.CROPS.id(),
                String.join(", ", seasonless)
        );
      }
    });
  }

  private static boolean hasNoSeason(RegistryEntry<Block> crop) {
    for (Season season : Season.values()) {
      if (crop.isIn(ModBlockTags.forSeason(season))) {
        return false;
      }
    }

    return true;
  }
}
