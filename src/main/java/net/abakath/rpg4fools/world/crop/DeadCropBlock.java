package net.abakath.rpg4fools.world.crop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What a crop leaves behind when its season ends.
 *
 * <p>One block for every crop. A dead melon stem and a dead carrot are the same thing to a player:
 * something to clear before replanting, worth nothing.
 *
 * <p>It never grows back and it never spreads. The random tick hook only looks at blocks in the
 * crops tag and this block is deliberately outside it, so nothing ever examines it again. Breaking
 * it is the only way it leaves the world.
 */
public class DeadCropBlock extends VegetationBlock {
  public static final MapCodec<DeadCropBlock> CODEC = simpleCodec(DeadCropBlock::new);

  /**
   * Flat and wide, following the sprite.
   *
   * <p>The art is trampled growth about six pixels deep, so a taller box would have the player
   * clicking empty air above it. Wide because collapsed stalks sprawl.
   */
  private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

  public DeadCropBlock(Properties settings) {
    super(settings);
  }

  @Override
  protected MapCodec<? extends VegetationBlock> codec() {
    return CODEC;
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  /**
   * Stands on dirt as readily as on farmland.
   *
   * <p>A crop pops off the moment its farmland reverts to dirt. Doing the same here would let a
   * player clear a dead field by trampling it, which is the opposite of leaving them something to
   * break.
   */
  @Override
  protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
    return floor.is(Blocks.FARMLAND) || floor.is(BlockTags.DIRT);
  }
}
