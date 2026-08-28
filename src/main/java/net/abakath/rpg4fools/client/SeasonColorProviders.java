package net.abakath.rpg4fools.client;

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
      return applySeasonTint(BiomeColors.getGrassColor(world, pos));
    }, GRASS_BLOCKS);

    // TODO: Add coloring to flowers, vines and more

    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (world == null || pos == null) {
        return -1;
      }
      return applySeasonTint(BiomeColors.getFoliageColor(world, pos));
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

  /**
   * Grades a biome colour by the current season. The tint is interpolated between the current sub
   * season and the next one across the month, so the year reads as a gradual shift rather than
   * twelve hard steps.
   */
  private static int applySeasonTint(int rgb) {
    SeasonTint current = SeasonTint.of(ClientSeasonState.getSubSeason());
    SeasonTint next = current.next();
    float t = ClientSeasonState.getProgress();

    float hueShift = lerp(current.getHueShift(), next.getHueShift(), t);
    float saturationFactor = lerp(current.getSaturationFactor(), next.getSaturationFactor(), t);
    float brightnessFactor = lerp(current.getBrightnessFactor(), next.getBrightnessFactor(), t);

    float[] hsb = HSB_SCRATCH.get();
    ColorMath.rgbToHsb(rgb, hsb);

    return ColorMath.hsbToRgb(
            hsb[0] + hueShift,
            hsb[1] * saturationFactor,
            hsb[2] * brightnessFactor
    );
  }

  private static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
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

}
