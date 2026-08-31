package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
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

  /**
   * Whether this is the last stick a column may have.
   *
   * <p>Drawn shorter when it is, which is the only way a player can tell a column is finished. Two
   * sticks and three sticks are otherwise the same picture with one more of it, and finding out the
   * cap by having a placement refused is a worse way to learn it.
   *
   * <p>Shared by both blocks in a column, since the post is drawn the same whether a crop is growing
   * up it or not.
   */
  public static final BooleanProperty CAPPED = BooleanProperty.of("capped");

  /**
   * Whether the plant that was growing here died on the stick.
   *
   * <p>Debris, and nothing more. A stick carrying dead growth is still an empty stick to every rule
   * that matters - it can be sown, a plant below can climb into it, and a field settling itself can
   * take it down - and the new growth is what clears it. Without this a season simply deleted the
   * plant, and a trellis that had been full one day was spotless the next.
   */
  public static final BooleanProperty DEAD = BooleanProperty.of("dead");

  /**
   * Where a block sits in its column.
   *
   * <p>Shared by both blocks, and a sprite in both. A living plant needs a foot with roots and a
   * crown that tapers; a dead one needs three variants of the same wreck so a column is not one
   * picture repeated. What counts as a neighbour differs between them, which is why each works the
   * value out for itself - see {@link #partAt} and its counterpart in {@link StickedCropBlock}.
   */
  public static final EnumProperty<ColumnPart> PART = EnumProperty.of("part", ColumnPart.class);

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
   * The topmost block of this position's column, whether or not anything is growing in it.
   *
   * <p>Where the next stick goes. {@link #plantTop} answers a different question - how tall the plant
   * is - and stops at the first empty stick.
   */
  public static BlockPos columnTop(BlockView world, BlockPos pos) {
    BlockPos cursor = pos;

    for (int step = 1; step < MAX_HEIGHT; step++) {
      if (!isColumn(world.getBlockState(cursor.up()))) {
        break;
      }

      cursor = cursor.up();
    }

    return cursor;
  }

  /**
   * How far up its column this position sits, counting from the farmland.
   *
   * <p>One for a stick on the ground, up to {@link #MAX_HEIGHT}. Stops counting at the cap: nothing
   * asks the difference between three and more, because more cannot be built.
   */
  public static int depth(BlockView world, BlockPos pos) {
    int depth = 1;
    BlockPos cursor = pos.down();

    while (depth < MAX_HEIGHT && isColumn(world.getBlockState(cursor))) {
      depth++;
      cursor = cursor.down();
    }

    return depth;
  }

  /** Whether a block at this position is the last one its column may hold. */
  public static boolean capped(BlockView world, BlockPos pos) {
    return depth(world, pos) >= MAX_HEIGHT;
  }

  /**
   * Where a stick sits in its column.
   *
   * <p>Counts any column block as a neighbour, unlike the plant's own version, which counts only
   * sections of itself. A stick above a living plant is the top of that column even though it is not
   * part of the plant, and should be drawn as one.
   */
  public static ColumnPart partAt(BlockView world, BlockPos pos) {
    boolean below = isColumn(world.getBlockState(pos.down()));
    boolean above = isColumn(world.getBlockState(pos.up()));

    if (below && above) {
      return ColumnPart.MIDDLE;
    }

    return below ? ColumnPart.TOP : above ? ColumnPart.BOTTOM : ColumnPart.SINGLE;
  }

  /**
   * A stick that knows where it stands, for anything putting one down.
   *
   * <p>Both derived properties are worked out here rather than left to a neighbour update, because a
   * neighbour update reaches the blocks around a placement and never the placement itself.
   */
  public static BlockState stick(BlockView world, BlockPos pos, boolean dead) {
    return ModBlocks.CROP_STICK.getDefaultState()
            .with(CAPPED, capped(world, pos))
            .with(PART, partAt(world, pos))
            .with(DEAD, dead);
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
