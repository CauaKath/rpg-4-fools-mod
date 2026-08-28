package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.SubSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.item.Items;

/**
 * Registers the season aware block and item tints.
 *
 * <p>Block tinting is purely a rendering concern, so this runs from the client entrypoint and is
 * registered exactly once. Colour providers are looked up on every chunk rebuild, which is what
 * makes the tint follow {@link ClientSeasonState} without any re-registration.
 */
@Environment(EnvType.CLIENT)
public final class SeasonColorProviders {
  private static final Block[] FOLIAGE_BLOCKS = {
          Blocks.OAK_LEAVES,
          Blocks.SPRUCE_LEAVES,
          Blocks.BIRCH_LEAVES,
          Blocks.JUNGLE_LEAVES,
          Blocks.ACACIA_LEAVES,
          Blocks.DARK_OAK_LEAVES,
          Blocks.MANGROVE_LEAVES
  };

  private static final Block[] GRASS_BLOCKS = {
          Blocks.GRASS_BLOCK,
          Blocks.SHORT_GRASS,
          Blocks.TALL_GRASS,
          Blocks.FERN,
          Blocks.LARGE_FERN
  };

  /**
   * Chunk building runs on several worker threads, so the scratch buffer used by the colour
   * conversion is per thread. This keeps the tint path free of allocations.
   */
  private static final ThreadLocal<float[]> HSB_SCRATCH = ThreadLocal.withInitial(() -> new float[3]);

  private SeasonColorProviders() {
  }

  public static void register() {
    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (world == null || pos == null) {
        return -1;
      }
      return adjustSaturation(BiomeColors.getGrassColor(world, pos), getSeasonSaturation());
    }, GRASS_BLOCKS);

    // TODO: Add coloring to flowers, vines and more

    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (world == null || pos == null) {
        return -1;
      }
      return adjustSaturation(BiomeColors.getFoliageColor(world, pos), getSeasonSaturation());
    }, FOLIAGE_BLOCKS);

    ColorProviderRegistry.ITEM.register((stack, tintIndex) -> 0x91BD59, Items.GRASS_BLOCK, Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN);
    ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
      Block block = Block.getBlockFromItem(stack.getItem());
      if (block != null) {
        return getDefaultFoliageColor(block);
      }
      return -1;
    }, FOLIAGE_BLOCKS);
  }

  private static int adjustSaturation(int rgb, float saturationFactor) {
    float[] hsb = HSB_SCRATCH.get();
    ColorMath.rgbToHsb(rgb, hsb);

    return ColorMath.hsbToRgb(hsb[0], hsb[1] * saturationFactor, hsb[2]);
  }

  private static int getDefaultFoliageColor(Block block) {
    if (block == Blocks.BIRCH_LEAVES) {
      return 0x80a755;
    } else if (block == Blocks.SPRUCE_LEAVES) {
      return 0x619961;
    } else if (block == Blocks.MANGROVE_LEAVES) {
      return 0x92c648;
    } else {
      return 0x48b518;
    }
  }

  private static float getSeasonSaturation() {
    SubSeason subSeason = ClientSeasonState.getSubSeason();

    return switch (subSeason) {
      case MID_WINTER -> 0.5f;
      case EARLY_WINTER, LATE_WINTER -> 0.7f;
      case LATE_AUTUMN, EARLY_SPRING -> 0.8f;
      case MID_SPRING, MID_AUTUMN -> 1.0f;
      case LATE_SPRING, EARLY_AUTUMN -> 1.2f;
      case EARLY_SUMMER, LATE_SUMMER -> 1.3f;
      case MID_SUMMER -> 1.5f;
    };
  }
}
