package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What grows back where nobody is tending the field.
 *
 * <p>A crop the season killed sows itself again unless a player put it there. That covers the
 * village farms this was written for without asking anything about villages: no structure to find,
 * no piece to measure, nothing outside the block. It also covers a crop that generated anywhere
 * else, which is the honest cost of the rule being this simple.
 *
 * <p>Winter offers nothing, so nothing regrows and the world browns over the way it should.
 */
public final class Resowing {
  /**
   * Crops nobody plants in a field.
   *
   * <p>Both are sniffer finds. Torchflower has to be named because its crop block extends CropBlock
   * like any grain; pitcher is a tall plant the filter drops anyway, and is named so that its
   * absence reads as a decision.
   */
  private static final Set<Block> NOT_SOWN = Set.of(Blocks.TORCHFLOWER_CROP, Blocks.PITCHER_CROP);

  /** How wide a patch of one crop is. Three blocks reads as a row somebody planted, not as noise. */
  private static final int PATCH = 3;

  private Resowing() {
  }

  /**
   * What would be sown here this season, or empty if the season has nothing to offer.
   *
   * <p>The choice is the same across a patch of three by three and changes with the season, so a
   * field comes back as bands of one crop rather than as a speckle, and comes back differently
   * after each winter. It is settled by position alone, so the sweep and the chunk scan cannot
   * disagree about a field one of them has already been over.
   */
  public static Optional<Block> sownAt(BlockPos pos, Season season) {
    List<Block> pool = inSeason(season);

    if (pool.isEmpty()) {
      return Optional.empty();
    }

    Random random = Random.create(MathHelper.hashCode(
            Math.floorDiv(pos.getX(), PATCH), season.ordinal(), Math.floorDiv(pos.getZ(), PATCH)));

    return Optional.of(pool.get(random.nextInt(pool.size())));
  }

  /**
   * Every crop the season keeps alive.
   *
   * <p>Read from the crops tag rather than a list of its own, so a datapack that retags a crop is
   * obeyed and this mod's own crops need no special mention. Narrowed to CropBlock, which drops the
   * stems, and then to what anyone would actually sow.
   */
  private static List<Block> inSeason(Season season) {
    List<Block> crops = new ArrayList<>();
    Optional<RegistryEntryList.Named<Block>> tagged = Registries.BLOCK.getEntryList(ModBlockTags.CROPS);

    if (tagged.isEmpty()) {
      return crops;
    }

    for (RegistryEntry<Block> entry : tagged.get()) {
      Block block = entry.value();
      BlockState state = block.getDefaultState();

      if (block instanceof CropBlock && !NOT_SOWN.contains(block) && CropSeasons.isInSeason(state, season)) {
        crops.add(block);
      }
    }

    return crops;
  }
}
