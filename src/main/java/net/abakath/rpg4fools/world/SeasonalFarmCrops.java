package net.abakath.rpg4fools.world;

import com.mojang.serialization.MapCodec;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.init.ModBlocks;
import net.abakath.rpg4fools.init.ModCrops;
import net.abakath.rpg4fools.init.ModProcessors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.List;

/**
 * Sows this mod's crops into a village farm, a lane at a time.
 *
 * <p>Replaces the rules that used to do this in the processor list files. Two things a datapack
 * rule cannot do are what this exists for: it can only decide one block at a time, so a farm came
 * out as a speckle of six different crops, and it cannot see the season, so a village generated in
 * winter came with a bed of crops that died on the first tick.
 *
 * <p>Runs before the vanilla rules rather than after. Those rules match on wheat, so a lane already
 * turned to tomato is left alone and the carrot and potato rates are untouched - the same bargain
 * the appended rules struck, kept by ordering instead.
 */
public class SeasonalFarmCrops extends StructureProcessor {
  public static final SeasonalFarmCrops INSTANCE = new SeasonalFarmCrops();
  public static final MapCodec<SeasonalFarmCrops> CODEC = MapCodec.unit(() -> INSTANCE);

  /** Roughly one lane in three, which is about what one plot in six came to before. */
  private static final float LANE_CHANCE = 1.0F / 3.0F;

  @Override
  protected StructureProcessorType<? extends StructureProcessor> getType() {
    return ModProcessors.SEASONAL_FARM_CROPS;
  }

  @Override
  public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos origin, BlockPos pivot,
                                                      StructureTemplate.StructureBlockInfo original,
                                                      StructureTemplate.StructureBlockInfo current,
                                                      StructurePlacementData data) {
    if (!current.state().isOf(Blocks.WHEAT)) {
      return current;
    }

    List<Block> crops = inSeason();
    if (crops.isEmpty()) {
      return current;
    }

    // Seeded from the farm's own corner and the lane's number, so every block of a lane reaches the
    // same answer without any of them knowing about the others, and two farms side by side do not
    // come out identical.
    int lane = VillageFarms.laneOf(current.pos(), origin, data.getRotation());
    Random random = Random.create(MathHelper.hashCode(origin.getX() + lane, origin.getY(), origin.getZ()));

    if (random.nextFloat() >= LANE_CHANCE) {
      return current;
    }

    Block crop = crops.get(random.nextInt(crops.size()));
    BlockState sown = crop.getDefaultState().with(CropBlock.AGE, current.state().get(CropBlock.AGE));

    return new StructureTemplate.StructureBlockInfo(current.pos(), sown, current.nbt());
  }

  /**
   * The crops that would survive being planted right now.
   *
   * <p>Empty in winter, which leaves the farm entirely vanilla. That is the point: a village is
   * built by villagers who would not have sown a crop the season kills, and a bed of dead crops on
   * a freshly generated farm reads as a bug rather than as weather.
   */
  private static List<Block> inSeason() {
    Season season = CurrentSeason.season();
    List<Block> crops = new ArrayList<>();

    for (CropDefinition definition : ModCrops.ALL) {
      if (definition.kind() != CropDefinition.Kind.FARMLAND) {
        continue;
      }

      Block crop = ModBlocks.blockFor(definition);

      if (CropSeasons.isInSeason(crop.getDefaultState(), season)) {
        crops.add(crop);
      }
    }

    return crops;
  }
}
