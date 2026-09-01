package net.abakath.rpg4fools.server.events;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    Registry<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
    Iterable<Holder<Block>> crops = blocks.getTagOrEmpty(ModBlockTags.CROPS);

    // A tag that never loaded looks exactly like a tag nobody put anything in, and both leave every
    // crop seasonless without a word anywhere. That silence is what let the tags ship in the wrong
    // directory unnoticed, so it gets a line of its own.
    if (!crops.iterator().hasNext()) {
      RPG4Fools.LOGGER.warn(
              "#{} loaded no blocks. Every crop will be treated as growing in every season. "
                      + "The mod's own tag files are missing or were not read.",
              ModBlockTags.CROPS.location()
      );
      return;
    }

    List<String> seasonless = new ArrayList<>();

    for (Holder<Block> crop : crops) {
      if (hasNoSeason(crop)) {
        seasonless.add(crop.unwrapKey().map(key -> key.identifier().toString()).orElse("unregistered block"));
      }
    }

    if (!seasonless.isEmpty()) {
      RPG4Fools.LOGGER.warn(
              "{} crop(s) in #{} carry no grows_in_* tag and will be treated as growing in every season: {}",
              seasonless.size(),
              ModBlockTags.CROPS.location(),
              String.join(", ", seasonless)
      );
    }
  }

  private static boolean hasNoSeason(Holder<Block> crop) {
    for (Season season : Season.values()) {
      if (crop.is(ModBlockTags.forSeason(season))) {
        return false;
      }
    }

    return true;
  }
}
