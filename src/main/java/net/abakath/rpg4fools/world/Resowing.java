package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlockTags;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 *
 * <p>The odd plant in a field of a crop sticks can carry comes back on a trellis, one to three sticks
 * tall. Decided per plant rather than per patch, unlike the crop itself: a lane of tomatoes with a
 * couple of trellises standing in it looks like a farm, and a lane where every plant has one looks
 * like a different feature entirely.
 *
 * <p>Only the sticks and a young plant are put down, never a grown one: the plant climbs them itself,
 * the same way a player's does.
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

  /**
   * One plant in twelve, where the patch came back as a crop sticks can carry.
   *
   * <p>Rare on purpose. A trellis is something a player builds, and a field full of them found in the
   * world would make the crafted stick pointless.
   */
  private static final int TRELLIS_CHANCE = 12;

  /** Out of ten trellises: six one stick, three two, one three. A full one stays worth finding. */
  private static final int TWO_STICKS_ABOVE = 6;
  private static final int THREE_STICKS_ABOVE = 9;

  /**
   * What to put down, and how tall a trellis to put it on.
   *
   * <p>Sticks are counted including the one the plant itself stands in, so one means a plant on a
   * single stick and zero means no trellis at all.
   */
  public record Sowing(BlockState crop, int sticks) {
  }

  private Resowing() {
  }

  /**
   * What would be sown here this season, or empty if the season has nothing to offer.
   *
   * <p>The crop is the same across a patch of three by three and changes with the season, so a field
   * comes back as bands of one crop rather than as a speckle, and comes back differently after each
   * winter. Whether a plant stands on a trellis is decided one plant at a time instead, so a band of
   * tomatoes is a band of tomatoes with the occasional trellis in it.
   *
   * <p>Both are settled by position alone, so the sweep and the chunk scan cannot disagree about a
   * field one of them has already been over.
   */
  public static Optional<Sowing> sownAt(BlockPos pos, Season season) {
    List<Block> pool = inSeason(season);

    if (pool.isEmpty()) {
      return Optional.empty();
    }

    RandomSource patch = RandomSource.create(Mth.getSeed(
            Math.floorDiv(pos.getX(), PATCH), season.ordinal(), Math.floorDiv(pos.getZ(), PATCH)));

    Block crop = pool.get(patch.nextInt(pool.size()));
    CropDefinition definition = ModBlocks.definitionFor(crop);

    if (definition == null || !definition.sticked()) {
      return Optional.of(new Sowing(crop.defaultBlockState(), 0));
    }

    // Seeded from the block rather than the patch, so trellises are scattered through a lane instead
    // of claiming all of one. Still position alone, so the sweep and the chunk scan cannot disagree
    // about a field one of them has already been over.
    RandomSource plant = RandomSource.create(Mth.getSeed(pos.getX(), pos.getY() + season.ordinal(), pos.getZ()));

    if (plant.nextInt(TRELLIS_CHANCE) != 0) {
      return Optional.of(new Sowing(crop.defaultBlockState(), 0));
    }

    return Optional.of(new Sowing(ModBlocks.stickedFor(definition).defaultBlockState(), sticks(plant)));
  }

  private static int sticks(RandomSource random) {
    int roll = random.nextInt(10);

    if (roll < TWO_STICKS_ABOVE) {
      return 1;
    }

    return roll < THREE_STICKS_ABOVE ? 2 : 3;
  }

  /**
   * Every crop the season keeps alive.
   *
   * <p>Read from the crops tag rather than a list of its own, so a datapack that retags a crop is
   * obeyed and this mod's own crops need no special mention. Narrowed to CropBlock, which drops the
   * stems, and then to what anyone would actually sow.
   *
   * <p>Sticked crops are kept out. They are in the crops tag and they are CropBlocks, so they would
   * otherwise be picked here - and a field would come back carrying trellises that were never built,
   * with nothing above them to climb. A trellis is decided deliberately in {@link #sownAt} instead.
   *
   * <p>Walled crops are kept out for a harder reason: there is no deliberate case to fall back on.
   * A wall is a build, not something a field puts up, so nothing here ever sows one - and a walled
   * cell sown into a field would be a cell with no wall around it and an address pointing at a root
   * that does not exist.
   */
  private static List<Block> inSeason(Season season) {
    List<Block> crops = new ArrayList<>();
    for (Holder<Block> entry : BuiltInRegistries.BLOCK.getTagOrEmpty(ModBlockTags.CROPS)) {
      Block block = entry.value();
      BlockState state = block.defaultBlockState();

      if (block instanceof CropBlock && !(block instanceof StickedCropBlock)
              && !(block instanceof WalledCropBlock)
              && !NOT_SOWN.contains(block) && CropSeasons.isInSeason(state, season)) {
        crops.add(block);
      }
    }

    return crops;
  }
}
