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
import net.minecraft.state.property.EnumProperty;
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

/**
 * One section of a crop climbing a trellis.
 *
 * <p>A plant may be up to three sections tall, but it is one plant: every section carries the same
 * age, they ripen together, and picking any of them picks all of them. Height is what the plant is
 * worth - a section's worth of fruit for each one - rather than three plants sharing a column.
 *
 * <p>Growing tall and growing ripe are separate stages, and height comes first. The plant stops at
 * the adult age for as long as there is an empty stick above it to claim, spending its rolls on
 * climbing instead. Only once there is nothing left to climb does it ripen. That ordering is also
 * what stops a player from banking a ripe plant and then stacking sticks onto it for free fruit:
 * a ripe plant is past the age that climbs, and cannot climb again until it has been picked.
 *
 * <p>The bottom section does all of it. It is the one that random ticks, and it writes its age up the
 * column, so nothing above it can drift out of step.
 */
public class StickedCropBlock extends RegrowingCropBlock {
  public static final MapCodec<StickedCropBlock> CODEC = createCodec(StickedCropBlock::new);

  /** Which end of the plant this section is, for the sprite. Worked out, never stored deliberately. */
  public static final EnumProperty<ColumnPart> PART = EnumProperty.of("part", ColumnPart.class);

  /**
   * One random tick in eight climbs a section.
   *
   * <p>Deliberately slower than growing an age. A plant that shot to full height the moment it was
   * grown would make the sticks a formality; making the player wait for each section is what turns
   * height into something earned.
   */
  private static final int CLIMB_CHANCE = 8;

  /** Matches the stick's box, since the plant is drawn around one. */
  private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

  public StickedCropBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(PART, ColumnPart.SINGLE));
  }

  @Override
  public MapCodec<StickedCropBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(PART);
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  /** Farmland for the bottom of a column, more column for everything above it. */
  @Override
  protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
    return floor.isOf(Blocks.FARMLAND) || CropSticks.isColumn(floor);
  }

  @Override
  public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    return super.canPlaceAt(state, world, pos) && CropSticks.canStand(world, pos);
  }

  /**
   * Pops off when its support goes, and otherwise works out which end of the plant it now is.
   *
   * <p>Deriving the part here rather than storing it when a section is placed is what keeps the
   * sprites honest. A plant that was cut in half, extended, or reduced to bare sticks by a season
   * fires neighbour updates on whatever is left, and each survivor asks the question again.
   */
  @Override
  public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                              WorldAccess world, BlockPos pos, BlockPos neighborPos) {
    BlockState updated = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);

    return updated.isAir() ? updated : updated.with(PART, partAt(world, pos));
  }

  /**
   * Grows the plant, from the bottom section only.
   *
   * <p>Everything above the bottom is along for the ride. Letting each section tick would have them
   * growing at their own rates, which is the stack of separate bushes this block exists not to be.
   *
   * <p>Only reached while the crop is in season. The random tick hook settles an out of season crop
   * at the head of the tick and cancels the rest of it, so there is no season check to make here.
   */
  @Override
  public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    // Asked of the world rather than read off the part, which is only ever a sprite. A section
    // loaded from a save written before the part existed would claim to be a whole plant, and three
    // of those in a column would grow at three different rates.
    if (CropSticks.isSticked(world.getBlockState(pos.down()))) {
      return;
    }

    if (world.getBaseLightLevel(pos, 0) < 9) {
      return;
    }

    int age = getAge(state);

    // Height before fruit. While there is a stick above the plant, every roll goes into climbing it.
    if (climbable(world, pos, age)) {
      if (random.nextInt(CLIMB_CHANCE) == 0) {
        climb(world, pos, age);
      }

      return;
    }

    if (age >= getMaxAge()) {
      return;
    }

    // Vanilla's roll, written out because vanilla measures moisture from directly under the crop and
    // the plant should grow at the rate its farmland allows however tall it has become.
    float moisture = getAvailableMoisture(this, world, CropSticks.base(world, pos));

    if (random.nextInt((int) (25.0F / moisture) + 1) == 0) {
      setPlantAge(world, pos, age + 1);
    }
  }

  /**
   * Bone meal, which the whole plant feels at once.
   *
   * <p>Spends itself on height while the plant still has a stick to climb, so meal is worth using on
   * a young trellis rather than only on a finished one. Written out rather than inherited because
   * vanilla's version rebuilds the state from the block's default, which would throw away the part
   * and leave a section drawing itself as the wrong end of the plant.
   */
  @Override
  protected void applyGrowth(World world, BlockPos pos, BlockState state) {
    BlockPos base = CropSticks.plantBase(world, pos);
    int age = getAge(world.getBlockState(base));

    if (climbable(world, base, age)) {
      climb(world, base, age);
      return;
    }

    setPlantAge(world, base, Math.min(age + getGrowthAmount(world), getMaxAge()));
  }

  /**
   * Picks the whole plant, wherever the player clicked it.
   *
   * <p>One plant, one harvest. Each section is worth what a tomato in the ground is worth, so a plant
   * three sections tall pays about three times over, and all of it lands at the player's feet in one
   * go rather than needing a click per section.
   *
   * <p>The plant is left standing at the adult age, exactly as picking a tomato in the ground leaves
   * it - and at that age it will climb again if the player has since added a stick.
   */
  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    // Nothing ripe to take. The parent handles bone meal and the empty click.
    if (!isMature(state)) {
      return super.onUse(state, world, pos, player, hit);
    }

    if (!(world instanceof ServerWorld serverWorld)) {
      return ActionResult.SUCCESS;
    }

    BlockPos base = CropSticks.plantBase(serverWorld, pos);
    int sections = CropSticks.plantHeight(serverWorld, base);
    int picked = 0;

    for (int section = 0; section < sections; section++) {
      picked += 1 + serverWorld.random.nextInt(3);
    }

    CropHarvest.drop(serverWorld, base, new ItemStack(produce(), picked));
    setPlantAge(serverWorld, base, adultAge());

    serverWorld.emitGameEvent(GameEvent.BLOCK_CHANGE, base,
            GameEvent.Emitter.of(player, serverWorld.getBlockState(base)));
    serverWorld.playSound(null, base, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS,
            1.0F, 0.8F + serverWorld.random.nextFloat() * 0.4F);

    return ActionResult.SUCCESS;
  }

  /**
   * Whether the plant has somewhere left to climb.
   *
   * <p>Only at the adult age. Younger and it is still becoming a plant; older and it has already
   * committed its growth to fruit, and would be handed a ripe section for free.
   */
  private boolean climbable(World world, BlockPos pos, int age) {
    if (age != adultAge()) {
      return false;
    }

    BlockPos top = CropSticks.plantTop(world, pos);

    return CropSticks.isEmpty(world.getBlockState(top.up()));
  }

  /** Claims the stick above the plant, extending it by one section at the age the rest is at. */
  private void climb(World world, BlockPos pos, int age) {
    BlockPos grown = CropSticks.plantTop(world, pos).up();

    // The part is set here as well as derived, so the new section is never drawn as a whole plant
    // for the tick before its neighbour update lands.
    world.setBlockState(grown, getDefaultState()
            .with(getAgeProperty(), age)
            .with(PART, ColumnPart.TOP), Block.NOTIFY_ALL);
  }

  /** Writes one age to every section, so the plant is never caught half ripe. */
  private void setPlantAge(World world, BlockPos pos, int age) {
    BlockPos base = CropSticks.plantBase(world, pos);

    for (int section = 0; section < CropSticks.MAX_HEIGHT; section++) {
      BlockPos at = base.up(section);
      BlockState state = world.getBlockState(at);

      if (!CropSticks.isSticked(state)) {
        return;
      }

      world.setBlockState(at, state.with(getAgeProperty(), age), Block.NOTIFY_LISTENERS);
    }
  }

  private static ColumnPart partAt(BlockView world, BlockPos pos) {
    boolean below = CropSticks.isSticked(world.getBlockState(pos.down()));
    boolean above = CropSticks.isSticked(world.getBlockState(pos.up()));

    if (below && above) {
      return ColumnPart.MIDDLE;
    }

    return below ? ColumnPart.TOP : above ? ColumnPart.BOTTOM : ColumnPart.SINGLE;
  }

  /**
   * The age this crop counts as grown at, which is the one a pick sets it back to.
   *
   * <p>Read off the roster rather than named here, so a second sticked crop with a different cycle
   * needs nothing added to this class.
   */
  private int adultAge() {
    return ModBlocks.definitionFor(this).regrowAge();
  }

  private Item produce() {
    return ModItems.produceItem(ModBlocks.definitionFor(this));
  }
}
