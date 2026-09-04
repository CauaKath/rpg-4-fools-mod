package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
  public static final MapCodec<CropWallBlock> CODEC = simpleCodec(CropWallBlock::new);

  /** The post down the middle, which is all an unjoined panel is. */
  private static final VoxelShape POST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);

  /** One arm reaching out to a neighbour, per direction, added to the post as the joins say. */
  private static final Map<Direction, VoxelShape> ARMS = arms();

  public CropWallBlock(Properties settings) {
    super(settings);

    BlockState state = defaultBlockState()
            .setValue(CropWalls.DEAD, false)
            .setValue(CropWalls.ARM, WallArm.CENTER)
            .setValue(CropWalls.ROW, 0);

    for (Direction direction : CropWalls.sideDirections()) {
      state = state.setValue(CropWalls.side(direction), false);
    }

    registerDefaultState(state);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(CropWalls.DEAD, CropWalls.ARM, CropWalls.ROW);

    for (Direction direction : CropWalls.sideDirections()) {
      builder.add(CropWalls.side(direction));
    }
  }

  @Override
  public MapCodec<CropWallBlock> codec() {
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
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    VoxelShape shape = POST;

    for (Direction direction : CropWalls.sideDirections()) {
      if (state.getValue(CropWalls.side(direction))) {
        shape = Shapes.or(shape, ARMS.get(direction));
      }
    }

    return shape;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return CropWalls.joins(context.getLevel(), context.getClickedPos(), defaultBlockState());
  }

  /**
   * Re-joins itself when the blocks beside it change, and never comes down.
   *
   * <p>No canPlaceAt to fail and so no path to popping off, which is the whole of the decorative
   * half: a wall someone built stays where they put it even if the ground under it is mined out.
   */
  @Override
  public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess,
                                BlockPos pos, Direction direction, BlockPos neighborPos,
                                BlockState neighborState, RandomSource random) {
    if (direction.getAxis().isVertical()) {
      return state;
    }

    return state.setValue(CropWalls.side(direction), CropWalls.joins(world, pos, direction));
  }

  private static Map<Direction, VoxelShape> arms() {
    Map<Direction, VoxelShape> arms = new EnumMap<>(Direction.class);

    arms.put(Direction.NORTH, Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 7.0));
    arms.put(Direction.SOUTH, Block.box(7.0, 0.0, 9.0, 9.0, 16.0, 16.0));
    arms.put(Direction.WEST, Block.box(0.0, 0.0, 7.0, 7.0, 16.0, 9.0));
    arms.put(Direction.EAST, Block.box(9.0, 0.0, 7.0, 16.0, 16.0, 9.0));

    return arms;
  }
}
