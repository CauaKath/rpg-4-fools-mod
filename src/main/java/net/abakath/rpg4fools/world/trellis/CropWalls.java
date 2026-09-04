package net.abakath.rpg4fools.world.trellis;

import net.abakath.rpg4fools.world.crop.WalledCropBlock;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The rules a crop wall and the plant on it stand by.
 *
 * <p>A wall is scenery. It joins its neighbours like a pane, needs no support, may be any size and
 * may be built anywhere, because half of what it is for is being a fence around a garden. Only the
 * plant cares about farmland, and only under the one cell it was sown in.
 *
 * <p>A plant is a box: three cells wide and three tall, with the sown cell at the bottom of the
 * middle column. It fills that box by spreading into whichever wall cell it can reach, so its shape
 * is never the same twice and a cell high in the box may have nothing at all beneath it. That is why
 * every cell stores its own address - {@link WallArm} across, row up - instead of finding the root by
 * walking, the way a stick column can.
 *
 * <p>Nothing is cached. A plant is at most nine cells and every question here is a handful of block
 * state reads, while a cache would have to be dropped on every break, placement, spread and season
 * turn.
 */
public final class CropWalls {
  /** How far to either side of its root a plant may reach. */
  public static final int REACH = 1;

  /** How many rows tall the box is, counting the root's own. */
  public static final int ROWS = 3;

  /**
   * Whether the vine that was growing here died on the wall.
   *
   * <p>Debris, and nothing more, exactly as {@link CropSticks#DEAD} is. A panel carrying dead growth
   * is still an empty panel to every rule that matters, and the new growth is what clears it. Without
   * it a season simply deleted the plant, and a wall that had been covered one day was spotless the
   * next.
   */
  public static final BooleanProperty DEAD = BooleanProperty.create("dead");

  /**
   * Which way the plant spreads.
   *
   * <p>Carried by the plant and not by the wall. A wall joins in all four directions so it can turn a
   * corner, and a plant that read its direction off those joins would have to pick one at a junction
   * anyway - so it picks once, when it is sown, and keeps the answer. A vine therefore never rounds a
   * corner even where the wall does, which is also the only way its box stays a box.
   *
   * <p>Nothing about the sprite depends on it. A cell is drawn as a crop cross, which looks the same
   * from every side, so this decides only where the plant may spread - and the blockstate never asks.
   */
  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

  /**
   * Which column of the box a cell sits in. See {@link WallArm}.
   *
   * <p>Carried by the panel as well as by the plant, so growth that died on a wall is drawn as the
   * cell it was rather than as one sprite repeated nine times. Meaningless on a panel that is not
   * {@link #DEAD}, in the same way {@link CropSticks#PART} is only worth reading on a stick that has
   * something on it.
   */
  public static final EnumProperty<WallArm> ARM = EnumProperty.create("arm", WallArm.class);

  /** How many blocks above the root a cell sits. Zero for the root's own row. Carried by both, as {@link #ARM} is. */
  public static final IntegerProperty ROW = IntegerProperty.create("row", 0, ROWS - 1);

  /** The pane joins, one per horizontal direction, exactly as vanilla panes carry them. */
  private static final Map<Direction, BooleanProperty> SIDES = sides();

  private CropWalls() {
  }

  /** A panel with nothing growing on it. */
  public static boolean isWall(BlockState state) {
    return state.is(ModBlocks.CROP_WALL);
  }

  /** One cell of a plant spread over a wall. */
  public static boolean isCrop(BlockState state) {
    return state.getBlock() instanceof WalledCropBlock;
  }

  /** Either of the two, which is what a wall joins onto and what a plant grows through. */
  public static boolean isPanel(BlockState state) {
    return isWall(state) || isCrop(state);
  }

  public static BooleanProperty side(Direction direction) {
    return SIDES.get(direction);
  }

  public static Iterable<Direction> sideDirections() {
    return SIDES.keySet();
  }

  /**
   * A bare panel that knows what it joins onto, for anything putting one down.
   *
   * <p>The joins are worked out here rather than left to a neighbour update, because a neighbour
   * update reaches the blocks around a placement and never the placement itself. Also used for the
   * panel a plant leaves behind, which arrives the same way: written straight into a cell, with
   * nothing having told it who its neighbours are.
   */
  public static BlockState wall(BlockGetter world, BlockPos pos, boolean dead) {
    return joins(world, pos, ModBlocks.CROP_WALL.defaultBlockState().setValue(DEAD, dead));
  }

  /**
   * The panel a cell of a plant leaves behind, keeping the address the plant held there.
   *
   * <p>The address is what lets the debris be drawn as the cell it was. A plant is shaped by where
   * its cells sit, so dead growth that forgot its arm and row would be the same sprite nine times
   * over and would read as nothing that ever grew.
   */
  public static BlockState dead(BlockGetter world, BlockPos pos, BlockState cell) {
    return joins(world, pos, ModBlocks.CROP_WALL.defaultBlockState()
            .setValue(DEAD, true)
            .setValue(ARM, cell.getValue(ARM))
            .setValue(ROW, cell.getValue(ROW)));
  }

  /** The same panel state with its four joins brought into line with what is actually around it. */
  public static BlockState joins(BlockGetter world, BlockPos pos, BlockState wall) {
    BlockState joined = wall;

    for (Direction direction : SIDES.keySet()) {
      joined = joined.setValue(SIDES.get(direction), joins(world, pos, direction));
    }

    return joined;
  }

  /**
   * Whether a panel here would reach out in this direction.
   *
   * <p>More panel, or anything with a flat face to meet - the bargain vanilla panes strike. A wall
   * that stopped short of the building it was leaning on would read as a mistake rather than as a
   * trellis.
   *
   * <p>Asked by both blocks. A panel and a panel with a vine on it are the same piece of wall and have
   * to reach the same answer, or the timber would change shape as a plant grew over it.
   */
  public static boolean joins(BlockGetter world, BlockPos pos, Direction direction) {
    BlockPos neighbour = pos.relative(direction);
    BlockState state = world.getBlockState(neighbour);

    return isPanel(state) || state.isFaceSturdy(world, neighbour, direction.getOpposite());
  }

  /**
   * The axis a plant sown into this cell would take.
   *
   * <p>Read off the wall, so a vine spreads along the run the player can see rather than across it.
   * A panel already carrying a plant has the answer stored; a bare one is asked which way it joins;
   * a spot with no panel in it at all - which is where a cucumber standing in the ground gets its
   * wall built around it - is judged by the panels beside it.
   *
   * <p>A junction, a corner and a lone panel are all genuinely ambiguous, and take the fallback: the
   * wall the player was facing when they sowed it.
   */
  public static Direction.Axis axisAt(BlockGetter world, BlockPos pos, Direction.Axis fallback) {
    BlockState state = world.getBlockState(pos);

    if (isCrop(state)) {
      return state.getValue(AXIS);
    }

    if (isWall(state)) {
      return decide(state.getValue(SIDES.get(Direction.EAST)) || state.getValue(SIDES.get(Direction.WEST)),
              state.getValue(SIDES.get(Direction.NORTH)) || state.getValue(SIDES.get(Direction.SOUTH)), fallback);
    }

    return decide(isPanel(world.getBlockState(pos.east())) || isPanel(world.getBlockState(pos.west())),
            isPanel(world.getBlockState(pos.north())) || isPanel(world.getBlockState(pos.south())), fallback);
  }

  /** One axis if exactly one of the two directions has wall in it, and the fallback otherwise. */
  private static Direction.Axis decide(boolean alongX, boolean alongZ, Direction.Axis fallback) {
    if (alongX == alongZ) {
      return fallback;
    }

    return alongX ? Direction.Axis.X : Direction.Axis.Z;
  }

  /** Whether this cell is the one the seed went into, which is the cell the plant is measured from. */
  public static boolean isRoot(BlockState state) {
    return isCrop(state) && state.getValue(ARM) == WallArm.CENTER && state.getValue(ROW) == 0;
  }

  /**
   * Where the root of the plant this cell belongs to sits.
   *
   * <p>One subtraction, because the cell stores its own address. No walking, so a cell with air below
   * it still finds its root, and no searching, so two plants grown into neighbouring cells cannot be
   * mistaken for one.
   */
  public static BlockPos root(BlockState state, BlockPos pos) {
    return pos.relative(positive(state.getValue(AXIS)), -state.getValue(ARM).offset()).below(state.getValue(ROW));
  }

  /** Where a given cell of the box sits in the world. */
  public static BlockPos cell(BlockPos root, Direction.Axis axis, WallArm arm, int row) {
    return root.relative(positive(axis), arm.offset()).above(row);
  }

  /**
   * Every cell the plant rooted here actually occupies.
   *
   * <p>A cell counts only if it is the same crop, on the same axis, and carrying the address of the
   * spot it is standing in. Anything else in the box is somebody else's - a second plant that grew
   * this far, most likely - and is not the plant's to ripen, pick or kill.
   */
  public static List<BlockPos> cells(BlockGetter world, BlockPos root, BlockState rootState) {
    Direction.Axis axis = rootState.getValue(AXIS);
    List<BlockPos> cells = new ArrayList<>();

    for (int row = 0; row < ROWS; row++) {
      for (int offset = -REACH; offset <= REACH; offset++) {
        WallArm arm = WallArm.at(offset);
        BlockPos pos = cell(root, axis, arm, row);

        if (holds(world, pos, rootState, arm, row)) {
          cells.add(pos);
        }
      }
    }

    return cells;
  }

  /**
   * Bare wall cells inside the box that the plant could spread into next.
   *
   * <p>Only cells touching the plant edge on, so a vine creeps rather than jumping a gap. Diagonals
   * are left out for that reason: a plant that could cross a corner would reach a cell it has no
   * visible connection to.
   */
  public static List<BlockPos> spreadable(BlockGetter world, BlockPos root, BlockState rootState) {
    Direction.Axis axis = rootState.getValue(AXIS);
    List<BlockPos> open = new ArrayList<>();

    for (int row = 0; row < ROWS; row++) {
      for (int offset = -REACH; offset <= REACH; offset++) {
        WallArm arm = WallArm.at(offset);
        BlockPos pos = cell(root, axis, arm, row);

        if (!isWall(world.getBlockState(pos))) {
          continue;
        }

        if (reaches(world, root, rootState, axis, offset, row)) {
          open.add(pos);
        }
      }
    }

    return open;
  }

  /** Whether any cell of the plant is directly beside this spot in the box. */
  private static boolean reaches(BlockGetter world, BlockPos root, BlockState rootState,
                                 Direction.Axis axis, int offset, int row) {
    return occupied(world, root, rootState, axis, offset - 1, row)
            || occupied(world, root, rootState, axis, offset + 1, row)
            || occupied(world, root, rootState, axis, offset, row - 1)
            || occupied(world, root, rootState, axis, offset, row + 1);
  }

  private static boolean occupied(BlockGetter world, BlockPos root, BlockState rootState,
                                  Direction.Axis axis, int offset, int row) {
    if (offset < -REACH || offset > REACH || row < 0 || row >= ROWS) {
      return false;
    }

    WallArm arm = WallArm.at(offset);

    return holds(world, cell(root, axis, arm, row), rootState, arm, row);
  }

  /** Whether this position holds the cell of this plant that belongs at this address. */
  private static boolean holds(BlockGetter world, BlockPos pos, BlockState rootState, WallArm arm, int row) {
    BlockState state = world.getBlockState(pos);

    return state.is(rootState.getBlock())
            && state.getValue(AXIS) == rootState.getValue(AXIS)
            && state.getValue(ARM) == arm
            && state.getValue(ROW) == row;
  }

  /** The direction an arm offset counts along, which is the axis' positive one. */
  public static Direction positive(Direction.Axis axis) {
    return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
  }

  private static Map<Direction, BooleanProperty> sides() {
    Map<Direction, BooleanProperty> sides = new EnumMap<>(Direction.class);

    sides.put(Direction.NORTH, BlockStateProperties.NORTH);
    sides.put(Direction.EAST, BlockStateProperties.EAST);
    sides.put(Direction.SOUTH, BlockStateProperties.SOUTH);
    sides.put(Direction.WEST, BlockStateProperties.WEST);

    return sides;
  }
}
