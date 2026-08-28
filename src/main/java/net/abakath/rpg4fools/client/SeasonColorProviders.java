package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.init.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;

/**
 * Registers the season aware block and item tints.
 *
 * <p>Block tinting is purely a rendering concern, so this runs from the client entrypoint and is
 * registered exactly once. Colour providers are looked up on every chunk rebuild, which is what
 * makes the tint follow {@link ClientSeasonState} without any re-registration.
 *
 * <p>Blocks fall into two groups. Most grass and leaf blocks are biome tinted, so the season grade
 * is applied on top of whatever {@link BiomeColors} returns. A few are not biome tinted in vanilla
 * and instead carry a fixed colour, so the grade is applied to that constant instead.
 */
@Environment(EnvType.CLIENT)
public final class SeasonColorProviders {
  /** Vanilla tint index for the tinted quads of grass and leaf models. */
  private static final int TINTED_LAYER = 0;

  private static final int DEFAULT_GRASS_COLOR = 0x91BD59;
  private static final int DEFAULT_FOLIAGE_COLOR = 0x48B518;
  private static final int BIRCH_FOLIAGE_COLOR = 0x80A755;
  private static final int SPRUCE_FOLIAGE_COLOR = 0x619961;
  private static final int MANGROVE_FOLIAGE_COLOR = 0x92C648;
  private static final int LILY_PAD_COLOR = 0x208030;

  /**
   * Brown for a dormant berry bush. Not season graded: the bush is brown because it is dormant, and
   * letting the season shift that colour would blur the one signal the block exists to give.
   */
  private static final int DORMANT_BUSH_COLOR = 0x7A6338;

  /** Blocks vanilla tints from the biome grass colour. */
  private static final Block[] GRASS_BLOCKS = {
          Blocks.GRASS_BLOCK,
          Blocks.SHORT_GRASS,
          Blocks.TALL_GRASS,
          Blocks.FERN,
          Blocks.LARGE_FERN,
          Blocks.POTTED_FERN,
          Blocks.SUGAR_CANE
  };

  /** Blocks vanilla tints from the biome foliage colour. */
  private static final Block[] FOLIAGE_BLOCKS = {
          Blocks.OAK_LEAVES,
          Blocks.JUNGLE_LEAVES,
          Blocks.ACACIA_LEAVES,
          Blocks.DARK_OAK_LEAVES,
          Blocks.MANGROVE_LEAVES,
          Blocks.VINE
  };

  /**
   * Chunk building runs on several worker threads, so the scratch buffer used by the colour
   * conversion is per thread. This keeps the tint path free of allocations.
   */
  private static final ThreadLocal<float[]> HSB_SCRATCH = ThreadLocal.withInitial(() -> new float[3]);

  private SeasonColorProviders() {
  }

  public static void register() {
    registerBiomeTintedBlocks();
    registerFixedColorBlocks();
    registerDormantBush();
    registerItems();
  }

  /**
   * The dormant bush reuses the vanilla bush texture and browns it, rather than shipping art or
   * borrowing the dead bush sprite, which would make it indistinguishable from a dead crop.
   */
  private static void registerDormantBush() {
    ColorProviderRegistry.BLOCK.register(
            (state, world, pos, tintIndex) -> tintIndex == TINTED_LAYER ? DORMANT_BUSH_COLOR : -1,
            ModBlocks.DORMANT_SWEET_BERRY_BUSH
    );
  }

  private static void registerBiomeTintedBlocks() {
    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (tintIndex != TINTED_LAYER) {
        return -1;
      }
      if (world == null || pos == null) {
        return applySeasonTint(DEFAULT_GRASS_COLOR);
      }
      return applySeasonTint(BiomeColors.getGrassColor(world, pos));
    }, GRASS_BLOCKS);

    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
      if (tintIndex != TINTED_LAYER) {
        return -1;
      }
      if (world == null || pos == null) {
        return applySeasonTint(DEFAULT_FOLIAGE_COLOR);
      }
      return applySeasonTint(BiomeColors.getFoliageColor(world, pos));
    }, FOLIAGE_BLOCKS);
  }

  /**
   * Birch, spruce and lily pads are not biome tinted in vanilla. They use a constant, so the season
   * grade is applied to that constant rather than to a biome lookup.
   *
   * <p>Cherry and azalea leaves are deliberately left out. Their models carry no tintindex, so a
   * colour provider would never be consulted. Tinting them means shipping model overrides, which is
   * left for a follow up. The same applies to flowers.
   */
  private static void registerFixedColorBlocks() {
    registerFixedColorBlock(Blocks.BIRCH_LEAVES, BIRCH_FOLIAGE_COLOR);
    registerFixedColorBlock(Blocks.SPRUCE_LEAVES, SPRUCE_FOLIAGE_COLOR);
    registerFixedColorBlock(Blocks.LILY_PAD, LILY_PAD_COLOR);
  }

  private static void registerFixedColorBlock(Block block, int baseColor) {
    ColorProviderRegistry.BLOCK.register(
            (state, world, pos, tintIndex) -> tintIndex == TINTED_LAYER ? applySeasonTint(baseColor) : -1,
            block
    );
  }

  /**
   * Inventory icons follow the season too. Previously the grass items were pinned to a constant and
   * the leaf items to their vanilla colour, so the inventory disagreed with the world.
   */
  private static void registerItems() {
    registerFixedColorItem(DEFAULT_GRASS_COLOR, Items.GRASS_BLOCK, Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN, Items.SUGAR_CANE);
    registerFixedColorItem(DEFAULT_FOLIAGE_COLOR, Items.OAK_LEAVES, Items.JUNGLE_LEAVES, Items.ACACIA_LEAVES, Items.DARK_OAK_LEAVES, Items.VINE);
    registerFixedColorItem(BIRCH_FOLIAGE_COLOR, Items.BIRCH_LEAVES);
    registerFixedColorItem(SPRUCE_FOLIAGE_COLOR, Items.SPRUCE_LEAVES);
    registerFixedColorItem(MANGROVE_FOLIAGE_COLOR, Items.MANGROVE_LEAVES);
    registerFixedColorItem(LILY_PAD_COLOR, Items.LILY_PAD);
  }

  private static void registerFixedColorItem(int baseColor, ItemConvertible... items) {
    ColorProviderRegistry.ITEM.register(
            (stack, tintIndex) -> tintIndex == TINTED_LAYER ? applySeasonTint(baseColor) : -1,
            items
    );
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
}
