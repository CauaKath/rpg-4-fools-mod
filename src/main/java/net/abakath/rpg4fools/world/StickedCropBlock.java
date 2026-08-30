package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * A crop growing up a trellis.
 *
 * <p>One block of a column, and the same plant at every height: the bottom one is sown from seed by
 * the player, and each one above is put there by the tier below it once that tier is an adult. Every
 * tier ripens on its own and is picked on its own, so a full column pays out three times over where
 * a bare plant pays once - which is what the sticks are bought for.
 *
 * <p>Extends the regrowing crop rather than the plain one, so picking a tier leaves it standing and
 * sets it back to the adult age, exactly as picking a tomato in the ground does. Height changes how
 * many plants there are to pick, not what picking one means.
 */
public class StickedCropBlock extends RegrowingCropBlock {
  public static final MapCodec<StickedCropBlock> CODEC = createCodec(StickedCropBlock::new);

  /**
   * One random tick in eight spreads an adult tier upward.
   *
   * <p>Deliberately slower than growing an age. A column that filled itself the moment the bottom
   * matured would make the sticks a formality; making the player wait for each tier is what turns
   * height into something earned rather than something bought.
   */
  private static final int SPREAD_CHANCE = 8;

  /** Matches the stick's box, since the plant is drawn inside one. */
  private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

  public StickedCropBlock(Settings settings) {
    super(settings);
  }

  @Override
  public MapCodec<StickedCropBlock> getCodec() {
    return CODEC;
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
   * Grows this tier, then tries to start the one above it.
   *
   * <p>Written out rather than delegated because vanilla measures moisture from directly under the
   * crop, which for anything but the bottom tier is another stick. The rest of the roll is vanilla's
   * unchanged, so a tier three blocks up grows at the same rate as one in the ground.
   *
   * <p>Only reached while the crop is in season. The random tick hook settles an out of season crop
   * at the head of the tick and cancels the rest of it, so there is no season check to make here.
   */
  @Override
  public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (world.getBaseLightLevel(pos, 0) < 9) {
      return;
    }

    spread(world, pos, random, grow(state, world, pos, random));
  }

  /** Advances the age by one on a successful roll, and reports the age the tier is left at. */
  private int grow(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    int age = getAge(state);

    if (age >= getMaxAge()) {
      return age;
    }

    float moisture = getAvailableMoisture(this, world, CropSticks.base(world, pos));

    if (random.nextInt((int) (25.0F / moisture) + 1) != 0) {
      return age;
    }

    world.setBlockState(pos, withAge(age + 1), Block.NOTIFY_LISTENERS);
    return age + 1;
  }

  /**
   * Puts this crop onto the empty stick above, as an adult.
   *
   * <p>Needs a stick the player has already stacked there: the plant climbs a trellis, it does not
   * build one. Nothing happens without it, and a stick added later is picked up on the next tick, so
   * a column can be raised a tier at a time.
   *
   * <p>The new tier starts at the age a pick leaves a plant at - grown, bare, and some way from
   * fruiting. Starting it from seed would make the second tier as slow as the first, and starting it
   * ripe would hand the player a harvest for placing a stick.
   */
  private void spread(ServerWorld world, BlockPos pos, Random random, int age) {
    if (age < adultAge()) {
      return;
    }

    BlockPos above = pos.up();

    if (!CropSticks.isEmpty(world.getBlockState(above))) {
      return;
    }

    if (random.nextInt(SPREAD_CHANCE) != 0) {
      return;
    }

    world.setBlockState(above, withAge(adultAge()), Block.NOTIFY_LISTENERS);
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
}
