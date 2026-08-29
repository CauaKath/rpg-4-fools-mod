package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * What a season does to a crop already in the ground.
 *
 * <p>Two callers ask this: the sweep that runs the moment a season turns, and the random tick hook
 * that catches whatever was not loaded at the time. Both have to reach the same answer, so the rules
 * live here rather than in either of them.
 *
 * <p>A village farm is the one place a crop is put back rather than left to die. Villagers tend
 * those fields, so a season turning there reads as replanting: the lane comes back as something
 * that grows now, and a lane that died over winter is sown again in spring. Winter itself has
 * nothing to sow, so villages get the same dead crops as everyone else.
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
    // Dead crops carry no crops tag, so they fall out of every rule below. A village is the one
    // place they are worth looking at again: the villagers who tended the field are still there.
    if (state.isOf(ModBlocks.DEAD_CROP)) {
      return replant(world, pos, season);
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

    if (inSeason) {
      return false;
    }

    if (replant(world, pos, season)) {
      return true;
    }

    world.setBlockState(pos, ModBlocks.DEAD_CROP.getDefaultState());
    return true;
  }

  /**
   * Sows a village lane with something the season keeps alive, and reports whether it did.
   *
   * <p>Says no for anything outside a village, and for every block in winter, when there is nothing
   * to sow. Both cases fall through to the crop dying, which is what a farm without a farmer does.
   *
   * <p>Placed at age 0. The crop is freshly sown, and a lane that came back already ripe would hand
   * a player a harvest for every season that turned.
   */
  private static boolean replant(ServerWorld world, BlockPos pos, Season season) {
    // Asked before the pool is built. Every dead crop in the world ticks now, and almost none of
    // them stand in a village; that case should cost a chunk lookup and nothing else.
    Optional<VillageFarms.Lane> lane = VillageFarms.laneAt(world, pos);

    if (lane.isEmpty()) {
      return false;
    }

    List<Block> pool = VillageFarms.inSeason(season);

    if (pool.isEmpty()) {
      return false;
    }

    world.setBlockState(pos, VillageFarms.sownIn(lane.get(), season, pool).getDefaultState());
    return true;
  }
}
