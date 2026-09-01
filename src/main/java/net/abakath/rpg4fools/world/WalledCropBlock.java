package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * One cell of a crop spread over a wall.
 *
 * <p>A plant may cover up to nine cells - three across, three up, with the sown cell at the bottom of
 * the middle column - but it is one plant: every cell carries the same age, they ripen together, and
 * picking any of them picks all of them. Coverage is what the plant is worth, a cell's worth of fruit
 * for each one, rather than nine plants sharing a wall.
 *
 * <p>Where it spreads next is a roll rather than an order. Any bare panel inside the box that touches
 * the plant edge on can be the next cell, so no two walls fill in the same shape and a half grown
 * plant looks like something creeping over a trellis rather than a rectangle being filled in. What it
 * cannot do is jump: diagonals are not neighbours, so a vine always reaches a cell from one it can
 * already be seen holding.
 *
 * <p>Spreading and ripening are separate stages, and spreading comes first, exactly as climbing comes
 * before fruit on a stick column. The plant sits at the adult age for as long as there is a panel left
 * to take, which is also what stops a player from banking a ripe plant and then building wall around
 * it for free fruit.
 *
 * <p>The root does all of it, but every cell ticks. A cell's random tick finds the root and runs the
 * root's decision, so a plant nine cells wide gets nine times the rolls and a big wall fills at a
 * rate worth building. Letting each cell grow on its own account instead is the stack of separate
 * bushes this block exists not to be.
 *
 * <p>A cell that cannot find its root turns back into a bare panel rather than popping off. That is
 * what a break looks like here: cut the root out and the vine goes, cell by cell, while the wall the
 * player built stays standing.
 */
public class WalledCropBlock extends RegrowingCropBlock {
  public static final MapCodec<WalledCropBlock> CODEC = createCodec(WalledCropBlock::new);

  /**
   * One roll in four takes a new cell.
   *
   * <p>Slower than growing an age, for the reason {@link StickedCropBlock}'s climb is: a plant that
   * sheeted across a wall the moment it was grown would make the wall a formality.
   */
  private static final int SPREAD_CHANCE = 4;

  /** The panel, drawn thick enough to click. One per axis, since the plant lies in a plane. */
  private static final VoxelShape X_SHAPE = Block.createCuboidShape(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
  private static final VoxelShape Z_SHAPE = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

  public WalledCropBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState()
            .with(CropWalls.AXIS, Direction.Axis.X)
            .with(CropWalls.ARM, WallArm.CENTER)
            .with(CropWalls.ROW, 0));
  }

  @Override
  public MapCodec<WalledCropBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(CropWalls.AXIS, CropWalls.ARM, CropWalls.ROW);
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return state.get(CropWalls.AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
  }

  /**
   * Farmland, and only under the root.
   *
   * <p>Every other cell is vine. Asking them for soil would mean a plant could only spread as wide as
   * the bed was tilled, and would say nothing at all about the six cells that are off the ground.
   */
  @Override
  protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
    return floor.isOf(Blocks.FARMLAND);
  }

  @Override
  public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    if (CropWalls.isRoot(state)) {
      return super.canPlaceAt(state, world, pos);
    }

    BlockState root = world.getBlockState(CropWalls.root(state, pos));

    return root.isOf(this) && CropWalls.isRoot(root) && root.get(CropWalls.AXIS) == state.get(CropWalls.AXIS);
  }

  /**
   * Turns back into a bare panel when it has nothing to belong to, instead of popping off.
   *
   * <p>Every other crop in this mod returns air here, which is what makes the loot drop. This one
   * would be dropping the wall out from under itself - the panel is the block, and the vine is only
   * covering it - so the cell reverts and the wall survives. Breaking the root therefore costs the
   * player the plant and none of the build, and the reversion spreads outward on its own as each
   * neighbour is updated in turn.
   */
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                             WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    if (!state.canPlaceAt(world, pos)) {
      return CropWalls.wall(world, pos, false);
    }

    return state;
  }

  /**
   * Grows the plant, from whichever cell was ticked.
   *
   * <p>One ladder, one rung per tick, decided at the root: fill in the cell that is still filling in,
   * then take another panel, and only when there is nothing left to take, ripen. A plant is therefore
   * never caught with fruit on one cell and none on another, and never caught having sprouted a
   * finished cell out of nowhere.
   *
   * <p>Only reached while the crop is in season. The random tick hook settles an out of season crop at
   * the head of the tick and cancels the rest of it, so there is no season check to make here.
   */
  @Override
  public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    BlockPos root = CropWalls.root(state, pos);
    BlockState rootState = world.getBlockState(root);

    // Stale address. The neighbour update that turns this cell back into a panel is already on its
    // way; nothing here should grow in the meantime.
    if (!rootState.isOf(this) || !CropWalls.isRoot(rootState)) {
      return;
    }

    // Measured at the root, like moisture. A cell in the shade of the wall it is growing on would
    // otherwise stall a plant whose bed is in full sun.
    if (world.getBaseLightLevel(root, 0) < 9) {
      return;
    }

    BlockPos filling = filling(world, root, rootState);

    if (filling != null) {
      grow(world, root, filling, random);
      return;
    }

    List<BlockPos> open = CropWalls.spreadable(world, root, rootState);

    if (!open.isEmpty()) {
      if (random.nextInt(SPREAD_CHANCE) == 0) {
        spread(world, root, rootState, open.get(random.nextInt(open.size())));
      }

      return;
    }

    int age = getAge(rootState);

    if (age >= getMaxAge()) {
      return;
    }

    if (rolls(world, root, random)) {
      setPlantAge(world, root, rootState, age + 1);
    }
  }

  /**
   * Bone meal, which follows the same ladder a tick does.
   *
   * <p>Spends itself on coverage while there is a panel left to take, so meal is worth using on a
   * young wall rather than only on a finished one. Written out rather than inherited because vanilla's
   * version rebuilds the state from the block's default, which would throw away the cell's address
   * and leave it looking for a root in the wrong place.
   *
   * <p>Growth below the adult age never overshoots it. That age is where the plant decides whether to
   * spread, and a handful of meal should not carry it straight past the decision.
   */
  @Override
  public void applyGrowth(World world, BlockPos pos, BlockState state) {
    BlockPos root = CropWalls.root(state, pos);
    BlockState rootState = world.getBlockState(root);

    if (!rootState.isOf(this) || !CropWalls.isRoot(rootState)) {
      return;
    }

    BlockPos filling = filling(world, root, rootState);

    if (filling != null) {
      feed(world, filling, world.getBlockState(filling));
      return;
    }

    List<BlockPos> open = CropWalls.spreadable(world, root, rootState);

    if (!open.isEmpty()) {
      spread(world, root, rootState, open.get(world.getRandom().nextInt(open.size())));
      return;
    }

    setPlantAge(world, root, rootState,
            Math.min(getAge(rootState) + getGrowthAmount(world), getMaxAge()));
  }

  /**
   * Picks the whole plant, wherever the player clicked it.
   *
   * <p>One plant, one harvest. Each cell is worth what a cucumber in the ground is worth, so a wall
   * nine cells across pays about nine times over, and all of it lands at the root's feet in one go
   * rather than needing a click per cell.
   *
   * <p>The plant is left at the adult age, exactly as picking a cucumber in the ground leaves it - and
   * at that age it will spread again if the player has since built more wall inside its reach.
   */
  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (!isMature(state)) {
      return super.onUse(state, world, pos, player, hit);
    }

    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.SUCCESS;
    }

    BlockPos root = CropWalls.root(state, pos);
    BlockState rootState = serverWorld.getBlockState(root);

    if (!rootState.isOf(this) || !CropWalls.isRoot(rootState)) {
      return super.onUse(state, world, pos, player, hit);
    }

    int cells = CropWalls.cells(serverWorld, root, rootState).size();
    int picked = 0;

    for (int cell = 0; cell < cells; cell++) {
      picked += 1 + serverWorld.random.nextInt(3);
    }

    CropHarvest.drop(serverWorld, root, new ItemStack(produce(), picked));
    setPlantAge(serverWorld, root, rootState, adultAge());

    serverWorld.emitGameEvent(GameEvent.BLOCK_CHANGE, root,
            GameEvent.Emitter.of(player, serverWorld.getBlockState(root)));
    serverWorld.playSound(null, root, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS,
            1.0F, 0.8F + serverWorld.random.nextFloat() * 0.4F);

    return ActionResult.SUCCESS;
  }

  /**
   * The cell that is still filling in, or null when the whole plant is at least adult.
   *
   * <p>There is only ever one, because a new cell is taken only once every cell is adult. Finding it
   * by looking rather than by remembering means a plant loaded from a save, or one whose shoot was
   * broken off, picks up wherever it actually is.
   */
  private BlockPos filling(WorldView world, BlockPos root, BlockState rootState) {
    for (BlockPos cell : CropWalls.cells(world, root, rootState)) {
      if (getAge(world.getBlockState(cell)) < adultAge()) {
        return cell;
      }
    }

    return null;
  }

  /** Advances one cell by one age, on a roll the plant's own bed decides. */
  private void grow(ServerWorld world, BlockPos root, BlockPos pos, Random random) {
    if (!rolls(world, root, random)) {
      return;
    }

    BlockState state = world.getBlockState(pos);

    world.setBlockState(pos, state.with(getAgeProperty(), getAge(state) + 1), Block.NOTIFY_LISTENERS);
  }

  /** The same, for bone meal, and never past the age where the plant decides to spread. */
  private void feed(World world, BlockPos pos, BlockState state) {
    int fed = Math.min(getAge(state) + getGrowthAmount(world), adultAge());

    world.setBlockState(pos, state.with(getAgeProperty(), fed), Block.NOTIFY_LISTENERS);
  }

  /**
   * Vanilla's growth roll, measured at the root.
   *
   * <p>Vanilla reads moisture from directly under the crop, and for any cell but the root that is air,
   * another panel, or whatever the wall happens to be standing on. Reading it at the root means the
   * plant grows at the rate its one bed allows however far it has spread.
   */
  private boolean rolls(ServerWorld world, BlockPos pos, Random random) {
    float moisture = getAvailableMoisture(this, world, pos);

    return random.nextInt((int) (25.0F / moisture) + 1) == 0;
  }

  /**
   * Takes a bare panel, putting a shoot in it.
   *
   * <p>At age zero, not at the age the rest of the plant is holding. A cell that appeared already
   * grown is what would make a covered wall look assembled rather than grown, and starting from
   * nothing is also what pays for the coverage: the shoot has to fill in before anything ripens.
   */
  private void spread(World world, BlockPos root, BlockState rootState, BlockPos target) {
    Direction.Axis axis = rootState.get(CropWalls.AXIS);
    int offset = axis == Direction.Axis.X ? target.getX() - root.getX() : target.getZ() - root.getZ();

    world.setBlockState(target, getDefaultState()
            .with(CropWalls.AXIS, axis)
            .with(CropWalls.ARM, WallArm.at(offset))
            .with(CropWalls.ROW, target.getY() - root.getY()), Block.NOTIFY_ALL);
  }

  /** Writes one age to every cell, so the plant is never caught half ripe. */
  private void setPlantAge(World world, BlockPos root, BlockState rootState, int age) {
    for (BlockPos cell : CropWalls.cells(world, root, rootState)) {
      BlockState state = world.getBlockState(cell);

      world.setBlockState(cell, state.with(getAgeProperty(), age), Block.NOTIFY_LISTENERS);
    }
  }

  /**
   * The age this crop counts as grown at, which is the one a pick sets it back to.
   *
   * <p>Read off the roster rather than named here, so a second walled crop with a different cycle
   * needs nothing added to this class.
   */
  private int adultAge() {
    return ModBlocks.definitionFor(this).regrowAge();
  }

  private Item produce() {
    return ModItems.produceItem(ModBlocks.definitionFor(this));
  }
}
