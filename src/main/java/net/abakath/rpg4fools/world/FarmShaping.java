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
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.registry.tag.BlockTags;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fits a farm to the ground it landed on, and stops two farms of the same shape looking alike.
 *
 * <p>Three jobs, all of them per column of the field so a plot never ends up with a crop and no
 * soil beneath it.
 *
 * <p>The first is the ground, judged twice. A plot whose ground is mostly wrong is not built at
 * all, since a farm trimmed down to three crops and a fence post is wreckage rather than a smaller
 * farm; a plot that is mostly right is trimmed a column at a time, dropping any that has no ground
 * within the tolerance of the field's level. Zero means level ground only, which is what the lane
 * farms want; one lets the blob farms follow gentle ground, and either way nothing is left hanging
 * over a cave or buried in a slope.
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
          Codec.FLOAT.fieldOf("fences").forGetter(shaping -> shaping.fences),
          Codec.FLOAT.fieldOf("coverage").forGetter(shaping -> shaping.coverage)
  ).apply(instance, FarmShaping::new));

  /**
   * Whether a plot's ground was good enough to build on, kept per placement.
   *
   * <p>The verdict is the same for every block of a piece and answering it means sampling the
   * ground across the whole plot, which is not a thing to do a thousand times over. Bounded rather
   * than grown without limit: worldgen runs on several threads and nothing here is worth a leak.
   */
  private static final Map<BlockBox, Boolean> VERDICTS = new ConcurrentHashMap<>();
  private static final int VERDICT_LIMIT = 256;

  /** How far apart the sample columns are when judging a plot. */
  private static final int SAMPLE_STEP = 2;

  private final int tolerance;
  private final float erosion;
  private final float fences;
  private final float coverage;

  public FarmShaping(int tolerance, float erosion, float fences, float coverage) {
    this.tolerance = tolerance;
    this.erosion = erosion;
    this.fences = fences;
    this.coverage = coverage;
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
    if (!worthBuilding(world, origin, data)) {
      return null;
    }

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
   * Whether this column has ground to stand on within the tolerance of the field's level.
   *
   * <p>Measured by looking at the blocks rather than by asking the heightmap. A heightmap answers
   * with the top of whatever is there, canopy included, and a village goes in before its own
   * chunk's trees but after the neighbouring chunks are finished - so in a forest the reading is
   * the treetops and every column looks hopeless. Reading the band directly asks the question that
   * was meant: is there ground here, at about the right height.
   *
   * <p>Measured against the piece's own y rather than the block's, so the whole column agrees: the
   * soil, the crop standing on it and the air above all live or die together.
   */
  private boolean suitsTheGround(WorldView world, BlockPos pos, BlockPos origin) {
    return hasGround(world, pos.getX(), origin.getY(), pos.getZ(), tolerance);
  }

  private static boolean hasGround(WorldView world, int x, int level, int z, int tolerance) {
    BlockPos.Mutable cursor = new BlockPos.Mutable();

    for (int y = level + tolerance; y >= level - tolerance; y--) {
      BlockState state = world.getBlockState(cursor.set(x, y, z));

      // Anything a plot could sit on. Leaves and grass are what grew there, not what is there.
      if (state.isSolidBlock(world, cursor) && !state.isIn(BlockTags.LEAVES)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Whether enough of this plot has ground to be worth building at all.
   *
   * <p>Trimming a column at a time is right for a farm that overruns a bank by a block. It is wrong
   * for a farm dropped on a hillside, which comes out as a handful of crops and a few fence posts
   * stepping down a slope - wreckage rather than a smaller farm. Below the coverage the plot asks
   * for, nothing is placed and the village keeps an empty lot.
   */
  private boolean worthBuilding(WorldView world, BlockPos origin, StructurePlacementData data) {
    BlockBox box = data.getBoundingBox();

    if (box == null) {
      return true;
    }

    Boolean known = VERDICTS.get(box);

    if (known != null) {
      return known;
    }

    int suitable = 0;
    int sampled = 0;

    for (int x = box.getMinX(); x <= box.getMaxX(); x += SAMPLE_STEP) {
      for (int z = box.getMinZ(); z <= box.getMaxZ(); z += SAMPLE_STEP) {
        sampled++;

        if (hasGround(world, x, origin.getY(), z, tolerance)) {
          suitable++;
        }
      }
    }

    boolean verdict = sampled == 0 || (float) suitable / sampled >= coverage;

    if (VERDICTS.size() > VERDICT_LIMIT) {
      VERDICTS.clear();
    }

    VERDICTS.put(box, verdict);

    return verdict;
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
