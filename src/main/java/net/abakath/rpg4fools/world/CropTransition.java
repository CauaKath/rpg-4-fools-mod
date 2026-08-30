package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.server.world.ServerWorld;
import net.abakath.rpg4fools.server.PlantedCrops;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * What a season does to a crop already in the ground.
 *
 * <p>Two callers ask this: the sweep that runs the moment a season turns, and the random tick hook
 * that catches whatever was not loaded at the time. Both have to reach the same answer, so the rules
 * live here rather than in either of them.
 *
 * <p>A crop the world planted is put back rather than left to die: it comes back as something that
 * grows now, and a field that died over winter is sown again in spring. A crop a player planted is
 * left exactly where it fell, because tending it is the player's business and a field that quietly
 * replaced itself would take that decision away. {@link net.abakath.rpg4fools.server.PlantedCrops}
 * is what tells the two apart.
 *
 * <p>Winter has nothing to sow, so everything browns over, tended or not.
 */
public class CropTransition {
  /** Whether {@link #apply} has anything to say about this block, which is what the sweep scans for. */
  public static boolean settles(BlockState state) {
    return CropSeasons.isCrop(state) || state.isOf(ModBlocks.DEAD_CROP);
  }

  /**
   * Brings one block into line with the season, and reports whether it changed.
   *
   * <p>The crops tag check comes first because the random tick hook calls this for every block in
   * the game.
   */
  public static boolean apply(ServerWorld world, BlockPos pos, BlockState state, Season season) {
    // Dead crops carry no crops tag, so they fall out of every rule below. They are still worth
    // looking at: a field nobody planted comes back the season after it died.
    if (state.isOf(ModBlocks.DEAD_CROP)) {
      return resow(world, pos, season);
    }

    if (!CropSeasons.isCrop(state)) {
      return false;
    }

    boolean inSeason = CropSeasons.isInSeason(state, season);

    // Bushes are the one crop that comes back, so they swap between two blocks instead of dying.
    Block dormant = DormantBushes.dormantOf(state.getBlock());
    if (dormant != null) {
      if (inSeason) {
        return false;
      }

      world.setBlockState(pos, dormant.getDefaultState());
      return true;
    }

    Block live = DormantBushes.liveOf(state.getBlock());
    if (live != null) {
      if (!inSeason) {
        return false;
      }

      // Age 1 is leafy with no fruit, so the bush picks up where a fresh one would rather than
      // handing back the berries it lost going dormant.
      world.setBlockState(pos, live.getDefaultState().with(SweetBerryBushBlock.AGE, 1));
      return true;
    }

    // A sticked crop leaves its trellis behind. The sticks were built by the player and are not
    // part of the plant, so an ending season takes the crop and returns the stick to standing
    // empty, ready to be sown again when the season comes back around. Every tier settles for
    // itself, so a full column comes down to a full column of empty sticks.
    if (CropSticks.isSticked(state)) {
      if (inSeason) {
        return false;
      }

      world.setBlockState(pos, ModBlocks.CROP_STICK.getDefaultState()
              .with(CropSticks.CAPPED, state.get(CropSticks.CAPPED)));
      return true;
    }

    if (inSeason) {
      return false;
    }

    if (resow(world, pos, season)) {
      return true;
    }

    world.setBlockState(pos, ModBlocks.DEAD_CROP.getDefaultState());
    return true;
  }

  /**
   * Sows this spot with something the season keeps alive, and reports whether it did.
   *
   * <p>Says no for anything a player planted, and for every block in winter, when there is nothing
   * to sow. Both cases fall through to the crop dying, which is what an untended field does.
   *
   * <p>Placed at age 0. The crop is freshly sown, and a field that came back already ripe would
   * hand a player a harvest for every season that turned.
   *
   * <p>A crop that came back on a trellis gets its sticks put up empty above it, and climbs them on
   * its own. Standing the plant up already grown would be the same free harvest by another route,
   * and would also make a world grown trellis behave unlike one a player built.
   */
  private static boolean resow(ServerWorld world, BlockPos pos, Season season) {
    if (PlantedCrops.get(world).planted(world, pos)) {
      return false;
    }

    Optional<Resowing.Sowing> sown = Resowing.sownAt(pos, season);

    if (sown.isEmpty()) {
      return false;
    }

    Resowing.Sowing sowing = sown.get();
    world.setBlockState(pos, sowing.crop());
    raise(world, pos, sowing.sticks());

    return true;
  }

  /**
   * Stands the rest of the trellis up above a crop that came back on one.
   *
   * <p>The plant's own block is the first stick, so only what is above it is put down here. Stops at
   * anything that is not air rather than clearing it: whatever is standing there was not part of the
   * field, and a shorter trellis is a better outcome than a field that eats what is built over it.
   */
  private static void raise(ServerWorld world, BlockPos pos, int sticks) {
    for (int above = 1; above < sticks; above++) {
      BlockPos at = pos.up(above);

      if (!world.getBlockState(at).isAir()) {
        return;
      }

      world.setBlockState(at, ModBlocks.CROP_STICK.getDefaultState()
              .with(CropSticks.CAPPED, CropSticks.capped(world, at)));
    }
  }
}
