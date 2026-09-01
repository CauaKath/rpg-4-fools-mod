package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
    // Bare sticks are worth looking at even though nothing about them is a crop. A trellis in a
    // village field is a plot waiting to be sown, and one left out of this scan would stand empty
    // for good.
    return CropSeasons.isCrop(state) || state.isOf(ModBlocks.DEAD_CROP) || CropSticks.isEmpty(state);
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

    // An empty trellis is a plot, not a crop, so it carries no tag and falls out of every rule
    // below. Sowing it again is the whole reason it is scanned: a village field that came back on
    // sticks would otherwise stand bare from the season that killed it onwards.
    if (CropSticks.isEmpty(state)) {
      return foot(world, pos) && resow(world, pos, season);
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

    // A walled crop leaves its wall behind, for the same reason a sticked one leaves its sticks: the
    // wall is not part of the plant. Nothing tries to sow the plot again, unlike every branch below.
    // A wall is a build rather than a plot - nobody put one up on behalf of a village - so a wall
    // that loses its crop stays a wall until a player sows it again.
    if (CropWalls.isCrop(state)) {
      if (inSeason) {
        return false;
      }

      world.setBlockState(pos, CropWalls.wall(world, pos, true));
      return true;
    }

    // A sticked crop leaves its trellis behind. The sticks are not part of the plant, so an ending
    // season takes the crop and returns the stick to standing empty. Every section settles for
    // itself, so a full column comes down to a full column of empty sticks - except the foot, which
    // gets the chance to sow the plot again first if nobody is tending it.
    if (CropSticks.isSticked(state)) {
      if (inSeason) {
        return false;
      }

      if (foot(world, pos) && resow(world, pos, season)) {
        return true;
      }

      // The plant stays on the stick, dead. Deleting it left a trellis that had been full one day
      // spotless the next, which read as the crop having been harvested rather than lost.
      world.setBlockState(pos, CropSticks.stick(world, pos, true));
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
    shape(world, pos, sowing.sticks());

    return true;
  }

  /**
   * Whether this is the block a column stands on, which is the only one that speaks for the plot.
   *
   * <p>Every block of a column is scanned, and all of them would otherwise try to sow the plot -
   * which for anything but the foot means a crop halfway up a trellis.
   */
  private static boolean foot(ServerWorld world, BlockPos pos) {
    return !CropSticks.isColumn(world.getBlockState(pos.down()));
  }

  /**
   * Brings the trellis above a resown plot to the height the plot asked for.
   *
   * <p>Puts sticks up and takes them down, because the season decides the crop afresh and last
   * season's trellis is no guide to this one's: a plot that came back as carrots has no use for one,
   * and a plot that came back on three sticks may only have had one before.
   *
   * <p>Only ever adds into air and only ever removes bare sticks. Whatever else is standing there
   * was not part of the field, and a trellis of the wrong height is a better outcome than a field
   * that eats what has been built over it.
   */
  private static void shape(ServerWorld world, BlockPos pos, int sticks) {
    for (int above = 1; above < CropSticks.MAX_HEIGHT; above++) {
      BlockPos at = pos.up(above);
      BlockState standing = world.getBlockState(at);

      if (above < sticks) {
        // Already a stick, and the plot wants one. Left alone rather than replaced, and carried
        // past rather than returned on: a taller trellis than the plot asked for still has surplus
        // above this, and stopping here would strand it.
        if (CropSticks.isEmpty(standing)) {
          // Clearing last season's remains along with it. The plot is being sown again, so the whole
          // trellis is put back in order rather than only the part being added to.
          if (standing.get(CropSticks.DEAD)) {
            world.setBlockState(at, standing.with(CropSticks.DEAD, false));
          }

          continue;
        }

        if (!standing.isAir()) {
          return;
        }

        world.setBlockState(at, CropSticks.stick(world, at, false));
        continue;
      }

      if (CropSticks.isEmpty(standing)) {
        world.setBlockState(at, Blocks.AIR.getDefaultState());
      }
    }
  }
}
