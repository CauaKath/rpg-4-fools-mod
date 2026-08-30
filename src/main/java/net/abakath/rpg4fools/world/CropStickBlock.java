package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * A trellis with nothing growing on it.
 *
 * <p>Placed on farmland or on another stick, up to three high. Sowing a crop into one swaps it for
 * that crop's sticked block, and a season ending swaps it back, so this is both where a column
 * starts and where it returns to. The sticks outlive the plant on purpose: a trellis is something
 * the player built, and taking it away every autumn would make it a consumable instead.
 *
 * <p>Carries no state. Which crop is on a stick is the block's identity rather than a property, and
 * how tall the column is can be counted off the world in three reads, so there is nothing left to
 * store.
 */
public class CropStickBlock extends Block {
  public static final MapCodec<CropStickBlock> CODEC = createCodec(CropStickBlock::new);

  /**
   * The post, and the full height of the block.
   *
   * <p>Narrow because there is only the one post to hit, and full height because the top of a stick
   * is exactly where the player aims to stack the next one.
   */
  private static final VoxelShape SHAPE = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

  public CropStickBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<CropStickBlock> getCodec() {
    return CODEC;
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  @Override
  public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    return CropSticks.canStand(world, pos);
  }

  /**
   * Pops off when whatever held it up goes, the way a plant does.
   *
   * <p>Returning air rather than removing the block is what makes the loot drop: the neighbour
   * update path breaks the block properly when the new state is air. That is also what gives a
   * column the behaviour of shortening from wherever it was cut - break the bottom and each block
   * above it comes down in turn, each dropping its own stick and whatever was growing on it.
   */
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                              WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    return state.canPlaceAt(world, pos)
            ? super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos)
            : Blocks.AIR.getDefaultState();
  }
}
