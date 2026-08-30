package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * The rules a stack of crop sticks stands by.
 *
 * <p>A column is one to three blocks tall, rooted on farmland, and every block in it is either an
 * empty stick or a crop growing on one. Both of those blocks ask the same questions - may I stand
 * here, where is my farmland - so the answers live here instead of being written twice and drifting.
 *
 * <p>Nothing is cached. A column is at most three blocks, so walking it is three block state reads,
 * and a cache would have to be invalidated by every break, placement and season turn.
 */
public final class CropSticks {
  /** How tall a column may be, counting the crop's own block. */
  public static final int MAX_HEIGHT = 3;

  private CropSticks() {
  }

  /** An empty stick nothing has been sown into. */
  public static boolean isEmpty(BlockState state) {
    return state.isOf(ModBlocks.CROP_STICK);
  }

  /** A crop growing on a stick. */
  public static boolean isSticked(BlockState state) {
    return state.getBlock() instanceof StickedCropBlock;
  }

  /** Either of the two, which is what counts towards the height and what supports the next one up. */
  public static boolean isColumn(BlockState state) {
    return isEmpty(state) || isSticked(state);
  }

  /**
   * Whether a column block may stand at this position.
   *
   * <p>Two things have to hold: the block underneath is farmland or more column, and the column the
   * position would belong to is no taller than {@link #MAX_HEIGHT}. Checking the height from here
   * rather than at placement time means a column can never be built past the cap by any route -
   * placing, growing upward, or a chunk loading with something odd in it.
   */
  public static boolean canStand(WorldView world, BlockPos pos) {
    BlockPos below = pos.down();
    BlockState floor = world.getBlockState(below);

    if (floor.isOf(Blocks.FARMLAND)) {
      return true;
    }

    if (!isColumn(floor)) {
      return false;
    }

    int height = 1;
    BlockPos cursor = below;

    while (isColumn(world.getBlockState(cursor))) {
      height++;

      if (height > MAX_HEIGHT) {
        return false;
      }

      cursor = cursor.down();
    }

    // Whatever the column ends on has to be farmland. A stack rooted on stone is not a column that
    // lost its bottom block, it is a stack that should never have been built.
    return world.getBlockState(cursor).isOf(Blocks.FARMLAND);
  }

  /**
   * The bottom section of the plant at this position.
   *
   * <p>Walks past sections only, so it stops at a bare stick. The bottom section is the one that
   * grows, ripens and decides how tall the plant is; everything above it follows.
   */
  public static BlockPos plantBase(BlockView world, BlockPos pos) {
    BlockPos cursor = pos;

    for (int step = 1; step < MAX_HEIGHT; step++) {
      if (!isSticked(world.getBlockState(cursor.down()))) {
        break;
      }

      cursor = cursor.down();
    }

    return cursor;
  }

  /** The topmost section of the plant, counted up from wherever this position sits in it. */
  public static BlockPos plantTop(BlockView world, BlockPos pos) {
    BlockPos cursor = pos;

    for (int step = 1; step < MAX_HEIGHT; step++) {
      if (!isSticked(world.getBlockState(cursor.up()))) {
        break;
      }

      cursor = cursor.up();
    }

    return cursor;
  }

  /** How many sections the plant standing on this base has, counted upward. */
  public static int plantHeight(BlockView world, BlockPos base) {
    int height = 0;

    while (height < MAX_HEIGHT && isSticked(world.getBlockState(base.up(height)))) {
      height++;
    }

    return height;
  }

  /**
   * The bottom block of this position's column, which is the one sitting on the farmland.
   *
   * <p>Growth reads moisture from the ground, and vanilla reads it from directly below the crop.
   * That is right for the bottom of a column and wrong for everything above it, where directly
   * below is another stick. Every tier measures the same bed by asking from here.
   */
  public static BlockPos base(BlockView world, BlockPos pos) {
    BlockPos cursor = pos;

    for (int step = 1; step < MAX_HEIGHT; step++) {
      if (!isColumn(world.getBlockState(cursor.down()))) {
        break;
      }

      cursor = cursor.down();
    }

    return cursor;
  }
}
