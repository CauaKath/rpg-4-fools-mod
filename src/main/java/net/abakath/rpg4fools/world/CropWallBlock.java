package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.block.ShapeContext;

import java.util.EnumMap;
import java.util.Map;

/**
 * A trellis panel with nothing growing on it.
 *
 * <p>Joins its neighbours like a pane and asks nothing else of the world: no support beneath it, no
 * farmland, no limit on how far it runs or how high it goes. That is deliberate and it is the
 * difference between this and a crop stick. A stick is a plot marker, so it is rooted and capped at
 * three; a wall is something a player builds - a garden fence, the side of a greenhouse - that a
 * cucumber happens to be able to climb. Only {@link WalledCropBlock} asks about farmland, and only
 * under the single cell a seed went into.
 *
 * <p>Its one stored property is dead growth left behind by a season, which is a sprite and nothing
 * else. Which crop covered a panel is the plant block's identity rather than anything kept here.
 *
 * <p>Deliberately outside the crops tag, so the season sweep never looks at it. An empty panel is not
 * a plot waiting to be sown the way an empty stick is: nobody put a wall up on behalf of a village,
 * so nothing should sow one.
 */
public class CropWallBlock extends Block {
  public static final MapCodec<CropWallBlock> CODEC = createCodec(CropWallBlock::new);

  /** The post down the middle, which is all an unjoined panel is. */
  private static final VoxelShape POST = Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

  /** One arm reaching out to a neighbour, per direction, added to the post as the joins say. */
  private static final Map<Direction, VoxelShape> ARMS = arms();

  public CropWallBlock(Settings settings) {
    super(settings);

    BlockState state = getDefaultState()
            .with(CropWalls.DEAD, false)
            .with(CropWalls.ARM, WallArm.CENTER)
            .with(CropWalls.ROW, 0);

    for (Direction direction : CropWalls.sideDirections()) {
      state = state.with(CropWalls.side(direction), false);
    }

    setDefaultState(state);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(CropWalls.DEAD, CropWalls.ARM, CropWalls.ROW);

    for (Direction direction : CropWalls.sideDirections()) {
      builder.add(CropWalls.side(direction));
    }
  }

  @Override
  public MapCodec<CropWallBlock> getCodec() {
    return CODEC;
  }

  /**
   * The post plus whatever arms it has, which is what the player can see.
   *
   * <p>No concession for the player holding a panel, unlike the crop stick: panels are placed against
   * each other's sides rather than stacked on a two pixel top face, so the ordinary outline is
   * already something you can hit.
   */
  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    VoxelShape shape = POST;

    for (Direction direction : CropWalls.sideDirections()) {
      if (state.get(CropWalls.side(direction))) {
        shape = VoxelShapes.union(shape, ARMS.get(direction));
      }
    }

    return shape;
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    return CropWalls.joins(context.getWorld(), context.getBlockPos(), getDefaultState());
  }

  /**
   * Re-joins itself when the blocks beside it change, and never comes down.
   *
   * <p>No canPlaceAt to fail and so no path to popping off, which is the whole of the decorative
   * half: a wall someone built stays where they put it even if the ground under it is mined out.
   */
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                             WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    if (direction.getAxis().isVertical()) {
      return state;
    }

    return state.with(CropWalls.side(direction), CropWalls.joins(world, pos, direction));
  }

  private static Map<Direction, VoxelShape> arms() {
    Map<Direction, VoxelShape> arms = new EnumMap<>(Direction.class);

    arms.put(Direction.NORTH, Block.createCuboidShape(7.0, 0.0, 0.0, 9.0, 16.0, 7.0));
    arms.put(Direction.SOUTH, Block.createCuboidShape(7.0, 0.0, 9.0, 9.0, 16.0, 16.0));
    arms.put(Direction.WEST, Block.createCuboidShape(0.0, 0.0, 7.0, 7.0, 16.0, 9.0));
    arms.put(Direction.EAST, Block.createCuboidShape(9.0, 0.0, 7.0, 16.0, 16.0, 9.0));

    return arms;
  }
}
