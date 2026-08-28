package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

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
public class DeadCropBlock extends PlantBlock {
  public static final MapCodec<DeadCropBlock> CODEC = createCodec(DeadCropBlock::new);

  /**
   * Flat and wide, following the sprite.
   *
   * <p>The art is trampled growth about six pixels deep, so a taller box would have the player
   * clicking empty air above it. Wide because collapsed stalks sprawl.
   */
  private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

  public DeadCropBlock(Settings settings) {
    super(settings);
  }

  @Override
  protected MapCodec<DeadCropBlock> getCodec() {
    return CODEC;
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
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
  protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
    return floor.isOf(Blocks.FARMLAND) || floor.isIn(BlockTags.DIRT);
  }
}
