package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.abakath.rpg4fools.init.ModProcessors;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldView;
import net.minecraft.registry.tag.BlockTags;
import org.jetbrains.annotations.Nullable;

/**
 * Fits a farm to the ground it landed on, and stops two farms of the same shape looking alike.
 *
 * <p>Three jobs, all of them per column of the field so a plot never ends up with a crop and no
 * soil beneath it.
 *
 * <p>The first is the ground. A jigsaw piece cannot refuse the spot it was given, so a farm that
 * lands on a hillside is trimmed to the part of the ground that suits it: any column whose surface
 * sits further than the tolerance from the field's own level is dropped before it is placed. Zero
 * means level ground only, which is what the lane farms want; one lets the blob farms follow gentle
 * ground, and either way nothing is left hanging over a cave or buried in a slope.
 *
 * <p>The second is the outline. The generator marks the outer ring of a blob field - farmland left
 * unwatered, wheat left at age zero - and each of those columns is dropped on a roll, so the shape
 * in the template is the largest a farm can be rather than the shape every farm has.
 *
 * <p>The third is the fence. The template rings the field with posts and most of them are taken
 * away again, which reads as a fence somebody put up and never finished.
 */
public class FarmShaping extends StructureProcessor {
  public static final MapCodec<FarmShaping> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
          Codec.INT.fieldOf("tolerance").forGetter(shaping -> shaping.tolerance),
          Codec.FLOAT.fieldOf("erosion").forGetter(shaping -> shaping.erosion),
          Codec.FLOAT.fieldOf("fences").forGetter(shaping -> shaping.fences)
  ).apply(instance, FarmShaping::new));

  private final int tolerance;
  private final float erosion;
  private final float fences;

  public FarmShaping(int tolerance, float erosion, float fences) {
    this.tolerance = tolerance;
    this.erosion = erosion;
    this.fences = fences;
  }

  @Override
  protected StructureProcessorType<? extends StructureProcessor> getType() {
    return ModProcessors.FARM_SHAPING;
  }

  @Override
  @Nullable
  public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos origin, BlockPos pivot,
                                                      StructureTemplate.StructureBlockInfo original,
                                                      StructureTemplate.StructureBlockInfo current,
                                                      StructurePlacementData data) {
    BlockPos pos = current.pos();

    if (!suitsTheGround(world, pos, origin)) {
      return null;
    }

    BlockState state = current.state();

    // Two rolls off the same column, kept apart so a field cell and the fence post beside it are
    // not deciding their fate from the same number.
    if (state.isIn(BlockTags.FENCES) || state.isIn(BlockTags.WALLS)) {
      return roll(pos, 1) < fences ? current : null;
    }

    if (isOutline(state) && roll(pos, 0) < erosion) {
      return null;
    }

    return current;
  }

  /**
   * Whether this column's surface is close enough to the field's level to be part of it.
   *
   * <p>Measured against the piece's own y rather than the block's, so the whole column agrees: the
   * soil, the crop standing on it and the air above all live or die together.
   *
   * <p>Read before the projection has had its say. A terrain matching piece is snapped to the
   * surface afterwards, which would make every column look like a perfect fit and this check like a
   * formality.
   */
  private boolean suitsTheGround(WorldView world, BlockPos pos, BlockPos origin) {
    int surface = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - 1;

    return Math.abs(surface - origin.getY()) <= tolerance;
  }

  /**
   * The outer ring of a blob field, which the generator marks rather than the processor guesses.
   *
   * <p>Unwatered soil and unsprouted wheat are the marks. Both are states the field would reach on
   * its own within a day, so a farm that keeps its outline looks like any other farm, and nothing
   * about the marking survives to be noticed.
   */
  private static boolean isOutline(BlockState state) {
    if (state.isOf(Blocks.FARMLAND)) {
      return state.get(FarmlandBlock.MOISTURE) == 0;
    }

    return state.isOf(Blocks.WHEAT) && state.get(CropBlock.AGE) == 0;
  }

  /** Stable per column and per world position, so a farm is shaped the same way every time it loads. */
  private static float roll(BlockPos pos, int salt) {
    return Random.create(MathHelper.hashCode(pos.getX(), salt, pos.getZ())).nextFloat();
  }
}
