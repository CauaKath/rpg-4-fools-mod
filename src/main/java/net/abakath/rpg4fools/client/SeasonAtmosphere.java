package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;

/**
 * Works out how strongly the current season applies at a position, and grades sky colour and fog
 * distance accordingly.
 *
 * <p>This is the shared logic behind the sky and fog hooks. It deliberately contains no rendering
 * code, so it can be reasoned about on its own.
 */
@Environment(EnvType.CLIENT)
public final class SeasonAtmosphere {
  /**
   * Biome temperature at or below which the season applies at full strength. Snowy biomes sit at
   * 0.0 and taiga at 0.25, so both are comfortably inside.
   */
  private static final float FULL_STRENGTH_TEMPERATURE = 0.6f;

  /**
   * Biome temperature at or above which the season has no effect. Desert and badlands sit at 2.0,
   * savanna at 1.2, so hot biomes fade out rather than cutting off at a hard border.
   */
  private static final float NO_STRENGTH_TEMPERATURE = 1.5f;

  /**
   * Shift used to key the biome cache. 2 gives a 4 block cell, fine enough that the blended value
   * moves smoothly as the player walks rather than stepping once per chunk section.
   */
  private static final int CACHE_CELL_SHIFT = 2;

  /** Horizontal reach of the blend kernel, in blocks. */
  private static final int SAMPLE_RADIUS = 24;

  /**
   * Horizontal offsets of the sample grid, in blocks. Five per axis rather than three: crossing a
   * taiga into a desert then moves the strength in steps of 0.2 instead of 0.33, which is the
   * difference between a visible step in the fog and a gradual change.
   */
  private static final int[] SAMPLE_OFFSETS = {
          -SAMPLE_RADIUS,
          -SAMPLE_RADIUS / 2,
          0,
          SAMPLE_RADIUS / 2,
          SAMPLE_RADIUS
  };

  /**
   * Single entry cache for the biome strength lookup. The fog hooks run per frame and the blend
   * costs 25 biome lookups, so it is recomputed only when the camera leaves its cache cell. Only
   * ever touched from the render thread.
   */
  private static long cachedCellKey = Long.MIN_VALUE;
  private static float cachedStrength = 1.0f;

  /** Scratch buffer for the colour conversion, kept per thread to stay allocation free. */
  private static final ThreadLocal<float[]> HSB_SCRATCH = ThreadLocal.withInitial(() -> new float[3]);

  private SeasonAtmosphere() {
  }

  /**
   * How much of the seasonal effect applies here, from 0 (hot biome, no effect) to 1 (cold biome,
   * full effect).
   *
   * <p>Averaged over a 5x5 horizontal grid rather than sampled at a single point. A single sample
   * makes the fog snap the instant the camera crosses a biome border, which is very visible walking
   * from a taiga into a desert. Averaging spreads that change over the width of the kernel.
   */
  public static float getSeasonStrength(WorldView world, BlockPos pos) {
    if (world == null || pos == null) {
      return 1.0f;
    }

    long cellKey = cacheCellKey(pos);

    if (cellKey == cachedCellKey) {
      return cachedStrength;
    }

    float strength = blendedStrength(world, pos);

    cachedCellKey = cellKey;
    cachedStrength = strength;

    return strength;
  }

  private static float blendedStrength(WorldView world, BlockPos pos) {
    BlockPos.Mutable samplePos = new BlockPos.Mutable();
    float total = 0.0f;

    for (int offsetX : SAMPLE_OFFSETS) {
      for (int offsetZ : SAMPLE_OFFSETS) {
        samplePos.set(pos.getX() + offsetX, pos.getY(), pos.getZ() + offsetZ);

        Biome biome = world.getBiome(samplePos).value();
        total += strengthForTemperature(biome.getTemperature());
      }
    }

    return total / (SAMPLE_OFFSETS.length * SAMPLE_OFFSETS.length);
  }

  static float strengthForTemperature(float temperature) {
    float span = NO_STRENGTH_TEMPERATURE - FULL_STRENGTH_TEMPERATURE;

    return ColorMath.clamp01((NO_STRENGTH_TEMPERATURE - temperature) / span);
  }

  /**
   * Grades a packed 0xRRGGBB sky colour by the current season, scaled by how strongly the season
   * applies at this position.
   */
  public static int gradeSkyColor(int rgb, float strength) {
    if (strength <= 0.0f) {
      return rgb;
    }

    float t = ClientSeasonState.getProgress();
    AtmosphereTint current = AtmosphereTint.of(ClientSeasonState.getSubSeason());
    AtmosphereTint next = current.next();

    float hueShift = lerp(current.getHueShift(), next.getHueShift(), t) * strength;
    float saturationFactor = applyStrength(lerp(current.getSaturationFactor(), next.getSaturationFactor(), t), strength);
    float brightnessFactor = applyStrength(lerp(current.getBrightnessFactor(), next.getBrightnessFactor(), t), strength);

    float[] hsb = HSB_SCRATCH.get();
    ColorMath.rgbToHsb(rgb, hsb);

    return ColorMath.hsbToRgb(
            hsb[0] + hueShift,
            hsb[1] * saturationFactor,
            hsb[2] * brightnessFactor
    );
  }

  /**
   * Vec3d flavour of {@link #gradeSkyColor(int, float)}, for the sky colour hook. Each component is
   * a 0..1 channel rather than a packed int.
   */
  public static Vec3d gradeSkyColor(Vec3d color, float strength) {
    int graded = gradeSkyColor(toPackedRgb(color), strength);

    return new Vec3d(
            ((graded >> 16) & 0xFF) / 255.0,
            ((graded >> 8) & 0xFF) / 255.0,
            (graded & 0xFF) / 255.0
    );
  }

  private static int toPackedRgb(Vec3d color) {
    int r = channelToByte(color.x);
    int g = channelToByte(color.y);
    int b = channelToByte(color.z);

    return (r << 16) | (g << 8) | b;
  }

  private static int channelToByte(double channel) {
    return (int) Math.round(ColorMath.clamp01((float) channel) * 255.0f);
  }

  /**
   * How far the player can see this season, as a factor on the vanilla fog distance. Below 1 pulls
   * the fog in.
   */
  public static float getFogDistanceFactor(float strength) {
    if (strength <= 0.0f) {
      return 1.0f;
    }

    float t = ClientSeasonState.getProgress();
    AtmosphereTint current = AtmosphereTint.of(ClientSeasonState.getSubSeason());
    AtmosphereTint next = current.next();

    return applyStrength(lerp(current.getFogDistanceFactor(), next.getFogDistanceFactor(), t), strength);
  }

  /**
   * Multiplier to apply to the fog start so the net result is the table's fogStartFactor relative
   * to the vanilla fog start. Pulled in harder than {@link #getFogDistanceFactor(float)} in the
   * cold months, so the haze fills the air around the player instead of sitting as a band on the
   * horizon.
   */
  public static float getFogStartFactor(float strength) {
    if (strength <= 0.0f) {
      return 1.0f;
    }

    float t = ClientSeasonState.getProgress();
    AtmosphereTint current = AtmosphereTint.of(ClientSeasonState.getSubSeason());
    AtmosphereTint next = current.next();

    float startFactor = applyStrength(lerp(current.getFogStartFactor(), next.getFogStartFactor(), t), strength);
    float distanceFactor = getFogDistanceFactor(strength);

    if (distanceFactor <= 0.0f) {
      return startFactor;
    }

    // The fog density hook has already scaled the view distance vanilla computed this start from,
    // so divide that back out. Both table columns then read as a plain fraction of the vanilla
    // value and can be tuned independently of each other.
    return startFactor / distanceFactor;
  }

  /** Eases a multiplier towards 1, which is the neutral value, as strength drops towards 0. */
  private static float applyStrength(float factor, float strength) {
    return lerp(1.0f, factor, strength);
  }

  private static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }

  private static long cacheCellKey(BlockPos pos) {
    long x = pos.getX() >> CACHE_CELL_SHIFT;
    long y = pos.getY() >> CACHE_CELL_SHIFT;
    long z = pos.getZ() >> CACHE_CELL_SHIFT;

    return (x & 0x3FFFFF) << 42 | (y & 0xFFFFF) << 22 | (z & 0x3FFFFF);
  }
}
