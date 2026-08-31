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

  /**
   * One random tick in four climbs a section.
   *
   * <p>Deliberately slower than growing an age. A plant that shot to full height the moment it was
   * grown would make the sticks a formality; making the player wait for each section is what turns
   * height into something earned.
   */
  private static final int CLIMB_CHANCE = 4;

  /** Matches the stick's box, since the plant is drawn around one. */
  private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

  public StickedCropBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(CropSticks.PART, ColumnPart.SINGLE).with(CropSticks.CAPPED, false));
  }

  @Override
  public MapCodec<StickedCropBlock> getCodec() {
    return CODEC;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(CropSticks.PART, CropSticks.CAPPED);
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

    if (updated.isAir()) {
      return updated;
    }

    return updated.with(CropSticks.PART, partAt(world, pos)).with(CropSticks.CAPPED, CropSticks.capped(world, pos));
  }

  /**
   * Grows the plant, from the bottom section only.
   *
   * <p>Everything above the bottom is along for the ride. Letting each section tick would have them
   * growing at their own rates, which is the stack of separate bushes this block exists not to be.
   *
   * <p>One ladder, one rung per tick, in order: fill the plant out, then put up a shoot on the next
   * stick, then fill that shoot in, and only when there is nothing left to climb, ripen. A plant is
   * therefore never caught with fruit on one section and none on another, and it is never caught
   * having sprouted a finished section out of nowhere.
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

    // Still becoming a plant. Nothing has climbed yet, so this is the whole of it.
    if (age < adultAge()) {
      grow(world, pos, state, random);
      return;
    }

    BlockPos top = CropSticks.plantTop(world, pos);
    BlockState crown = world.getBlockState(top);

    // A shoot the plant put up is still filling in. It thickens on its own rolls while the rest of
    // the plant waits at the adult age, which is what makes a plant look like it is growing into its
    // trellis rather than arriving in it.
    if (getAge(crown) < adultAge()) {
      grow(world, top, crown, random);
      return;
    }

    // Height before fruit. While there is a stick above the plant, every roll goes into climbing it.
    if (CropSticks.isEmpty(world.getBlockState(top.up()))) {
      if (random.nextInt(CLIMB_CHANCE) == 0) {
        climb(world, top);
      }

      return;
    }

    if (age >= getMaxAge()) {
      return;
    }

    if (rolls(world, pos, random)) {
      setPlantAge(world, pos, age + 1);
    }
  }

  /**
   * Advances one section by one age.
   *
   * <p>Used for the two stages that are local to a section rather than shared: a plant that has not
   * reached the adult age yet, and a shoot filling in above one that has.
   */
  private void grow(ServerWorld world, BlockPos pos, BlockState state, Random random) {
    if (!rolls(world, pos, random)) {
      return;
    }

    world.setBlockState(pos, state.with(getAgeProperty(), getAge(state) + 1), Block.NOTIFY_LISTENERS);
  }

  /**
   * Vanilla's growth roll, written out.
   *
   * <p>Vanilla measures moisture from directly under the crop, and for any section but the bottom
   * that is another stick. Measuring from the farmland the column is rooted in means the plant grows
   * at the rate its bed allows however tall it has become.
   */
  private boolean rolls(ServerWorld world, BlockPos pos, Random random) {
    float moisture = getAvailableMoisture(this, world, CropSticks.base(world, pos));

    return random.nextInt((int) (25.0F / moisture) + 1) == 0;
  }

  /**
   * Bone meal, which follows the same ladder a tick does.
   *
   * <p>Spends itself on height while the plant still has a stick to climb, so meal is worth using on
   * a young trellis rather than only on a finished one. Written out rather than inherited because
   * vanilla's version rebuilds the state from the block's default, which would throw away the part
   * and leave a section drawing itself as the wrong end of the plant.
   *
   * <p>Growth below the adult age never overshoots it. That age is where the plant decides whether to
   * climb, and a handful of meal should not carry it straight past the decision.
   */
  @Override
  public void applyGrowth(World world, BlockPos pos, BlockState state) {
    BlockPos base = CropSticks.plantBase(world, pos);
    BlockState root = world.getBlockState(base);
    int age = getAge(root);

    if (age < adultAge()) {
      feed(world, base, root, adultAge());
      return;
    }

    BlockPos top = CropSticks.plantTop(world, base);
    BlockState crown = world.getBlockState(top);

    if (getAge(crown) < adultAge()) {
      feed(world, top, crown, adultAge());
      return;
    }

    if (CropSticks.isEmpty(world.getBlockState(top.up()))) {
      climb(world, top);
      return;
    }

    setPlantAge(world, base, Math.min(age + getGrowthAmount(world), getMaxAge()));
  }

  private void feed(World world, BlockPos pos, BlockState state, int ceiling) {
    int fed = Math.min(getAge(state) + getGrowthAmount(world), ceiling);

    world.setBlockState(pos, state.with(getAgeProperty(), fed), Block.NOTIFY_LISTENERS);
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
   * Claims the stick above the plant, putting up a shoot in it.
   *
   * <p>At age zero, not at the age the rest of the plant is holding. A section that appeared already
   * grown was the thing that made a tall plant look assembled rather than grown, and starting from
   * nothing is also what pays for the height: the shoot has to fill in before anything ripens.
   */
  private void climb(World world, BlockPos pos) {
    BlockPos grown = pos.up();

    // The part is set here as well as derived, so the new section is never drawn as a whole plant
    // for the tick before its neighbour update lands.
    world.setBlockState(grown, getDefaultState()
            .with(CropSticks.PART, ColumnPart.TOP)
            .with(CropSticks.CAPPED, CropSticks.capped(world, grown)), Block.NOTIFY_ALL);
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
