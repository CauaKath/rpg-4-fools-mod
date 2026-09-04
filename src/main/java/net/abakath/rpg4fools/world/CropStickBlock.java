package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A trellis with nothing growing on it.
 *
 * <p>Placed on farmland or on another stick, up to three high. Sowing a crop into one swaps it for
 * that crop's sticked block, and a season ending swaps it back, so this is both where a column
 * starts and where it returns to. The sticks outlive the plant on purpose: a trellis is something
 * the player built, and taking it away every autumn would make it a consumable instead.
 *
 * <p>Its two properties are both sprites and nothing else. The last stick a column may hold is drawn
 * shorter, so a finished column says so rather than making the player find out by having a placement
 * refused; and a stick whose plant a season killed keeps the dead growth until something replaces it.
 * Which crop is on a stick is the block's identity rather than a property.
 */
public class CropStickBlock extends Block {
  public static final MapCodec<CropStickBlock> CODEC = simpleCodec(CropStickBlock::new);

  /**
   * The post.
   *
   * <p>Narrow because there is only the one post to hit, and full height because the top of a stick
   * is exactly where the player aims to stack the next one. The capped shape follows its shorter
   * post: there is nothing to stack on it, so nothing is lost by being harder to click on top of.
   */
  private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
  private static final VoxelShape CAPPED_SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 9.0, 10.0);

  public CropStickBlock(Properties settings) {
    super(settings);
    registerDefaultState(defaultBlockState()
            .setValue(CropSticks.CAPPED, false)
            .setValue(CropSticks.PART, ColumnPart.SINGLE)
            .setValue(CropSticks.DEAD, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(CropSticks.CAPPED, CropSticks.PART, CropSticks.DEAD);
  }

  @Override
  public MapCodec<CropStickBlock> codec() {
    return CODEC;
  }

  /**
   * The post, or the whole block to anyone holding a stick.
   *
   * <p>The same bargain scaffolding strikes. Stacking a column means clicking a post two pixels wide,
   * and a player carrying sticks is trying to build with them, so the block becomes as easy to hit as
   * it is to reason about. Anyone holding anything else gets the post they can see.
   */
  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    if (context.isHoldingItem(asItem())) {
      return Shapes.block();
    }

    return state.getValue(CropSticks.CAPPED) ? CAPPED_SHAPE : SHAPE;
  }

  /**
   * Works out on the way in whether this is the column's last stick.
   *
   * <p>Neighbour updates reach the blocks around a placement, never the placement itself, so a stick
   * that arrives on top of two others would otherwise draw itself full height until something else
   * disturbed it.
   */
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return CropSticks.stick(context.getLevel(), context.getClickedPos(), false);
  }

  @Override
  public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
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
  public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                              LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    if (!state.canSurvive(world, pos)) {
      return Blocks.AIR.defaultBlockState();
    }

    return super.updateShape(state, direction, neighborState, world, pos, neighborPos)
            .setValue(CropSticks.CAPPED, CropSticks.capped(world, pos))
            .setValue(CropSticks.PART, CropSticks.partAt(world, pos));
  }
}
