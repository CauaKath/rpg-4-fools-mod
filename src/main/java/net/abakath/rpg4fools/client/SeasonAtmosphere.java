package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.SubSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
   * Shift used to key the biome cache. 2 gives a 4 block cell. It does not need to be finer than
   * that: FogTransition eases between whatever this reports, so continuity comes from the easing
   * rather than from resampling more often.
   */
  private static final int CACHE_CELL_SHIFT = 2;

  /** At or above this Y the cave atmosphere never applies, even with no sky overhead. */
  private static final int CAVE_FADE_TOP = 60;

  /** At or below this Y the cave atmosphere applies in full. */
  private static final int CAVE_FADE_BOTTOM = 40;

  /** Horizontal reach of the blend kernel, in blocks. */
  private static final int SAMPLE_RADIUS = 40;

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
  private static long cachedAggregateCellKey = Long.MIN_VALUE;
  private static BiomeAggregate cachedAggregate = null;

  /** Scratch buffer for the colour conversion, kept per thread to stay allocation free. */
  private static final ThreadLocal<float[]> HSB_SCRATCH = ThreadLocal.withInitial(() -> new float[3]);

  /**
   * The interpolated atmosphere grade, held alongside the date it was worked out for. The factors
   * move once per in game day, but the fog and sky hooks were re-interpolating them every frame.
   *
   * <p>The strength scaling is deliberately left out. It varies per call, so only the part that
   * depends on the date alone is worth holding.
   */
  private record Grade(SubSeason subSeason, float progress, float hueShift,
                       float saturationFactor, float brightnessFactor, float fogPresenceBoost) {
  }

  private static volatile Grade grade = gradeFor(SubSeason.EARLY_SPRING, 0.0f);

  private SeasonAtmosphere() {
  }

  /**
   * The biome half of the atmosphere, averaged over the sample grid. Cached on its own because it
   * depends only on where the player is, never on the date. Keeping the season out of the cache
   * means a sub season change can never serve a stale value.
   *
   * @param effectiveStrength family season sensitivity multiplied by the temperature curve
   */
  /**
   * The biome half of the atmosphere, averaged over the sample grid. Cached on its own because it
   * depends only on where the player is, never on the date. Keeping the season out of the cache
   * means a sub season change can never serve a stale value.
   *
   * @param effectiveStrength family season sensitivity multiplied by the temperature curve
   */
  private record BiomeAggregate(int tintColor, float colorBlend, float effectiveStrength,
                                float fogPresence, float fogStart, float fogEnd) {
  }

  /**
   * Resolves the finished atmosphere at a position.
   *
   * <p>This is the single place the fog inputs compose: a family's presence, what the season adds
   * to it, what the weather takes off the distances, and whether a cave overrides all of it.
   */
  public static ResolvedAtmosphere resolve(WorldView world, BlockPos pos) {
    BiomeAggregate aggregate = aggregateFor(world, pos);

    float surfacePresence = surfaceFogPresence(aggregate);

    // Weather tightens the fog by pulling the target distances in, not by raising presence. Several
    // families already sit at presence 1, so a multiplier there would clamp, and a snowy biome mid
    // snowfall would look identical to the same biome under a clear sky.
    float weather = world instanceof World ? WeatherFog.multiplierAt((World) world, pos) : 1.0f;
    float surfaceStart = aggregate.fogStart() / weather;
    float surfaceEnd = aggregate.fogEnd() / weather;

    float caveFactor = caveFactor(world, pos);

    if (caveFactor <= 0.0f) {
      return new ResolvedAtmosphere(
              aggregate.tintColor(),
              aggregate.colorBlend(),
              aggregate.effectiveStrength(),
              surfacePresence,
              surfaceStart,
              surfaceEnd
      );
    }

    CaveAtmosphere cave = CaveAtmosphere.of(world.getBiome(pos));

    return new ResolvedAtmosphere(
            ColorMath.lerpRgb(aggregate.tintColor(), cave.getTintColor(), caveFactor),
            lerp(aggregate.colorBlend(), cave.getColorBlend(), caveFactor),
            lerp(aggregate.effectiveStrength(), 0.0f, caveFactor),
            lerp(surfacePresence, cave.getFogPresence(), caveFactor),
            lerp(surfaceStart, cave.getFogStart(), caveFactor),
            lerp(surfaceEnd, cave.getFogEnd(), caveFactor)
    );
  }

  /**
   * Fog presence at the surface: the family baseline plus what the season adds.
   *
   * <p>The season adds rather than multiplies on purpose. An open biome sits at a zero baseline, so
   * multiplying would leave it clear all year. Adding is what gives plains thin cold fog in winter
   * while keeping it completely clear in summer.
   */
  private static float surfaceFogPresence(BiomeAggregate aggregate) {
    float boost = grade().fogPresenceBoost();

    return ColorMath.clamp01(aggregate.fogPresence() + boost * aggregate.effectiveStrength());
  }

  /** The grade for the current date, recomputed only when the date has moved since the last call. */
  private static Grade grade() {
    SubSeason subSeason = ClientSeasonState.getSubSeason();
    float progress = ClientSeasonState.getProgress();

    Grade cached = grade;
    if (cached.subSeason() == subSeason && cached.progress() == progress) {
      return cached;
    }

    Grade fresh = gradeFor(subSeason, progress);
    grade = fresh;

    return fresh;
  }

  private static Grade gradeFor(SubSeason subSeason, float progress) {
    AtmosphereTint current = AtmosphereTint.of(subSeason);
    AtmosphereTint next = current.next();

    return new Grade(
            subSeason,
            progress,
            lerp(current.getHueShift(), next.getHueShift(), progress),
            lerp(current.getSaturationFactor(), next.getSaturationFactor(), progress),
            lerp(current.getBrightnessFactor(), next.getBrightnessFactor(), progress),
            lerp(current.getFogPresenceBoost(), next.getFogPresenceBoost(), progress)
    );
  }

  /**
   * How much the cave atmosphere applies here, from 0 at the surface to 1 deep underground.
   *
   * <p>Sky visibility alone is not enough, or standing inside a house would read as a cave. The Y
   * band on top of it also gives the transition somewhere to happen, so walking down a mineshaft
   * fades into the cave atmosphere instead of snapping to it.
   *
   * <p>Deliberately outside the biome cache: this depends on exact Y and on sky access, both of
   * which change far faster than the cache cell.
   */
  private static float caveFactor(WorldView world, BlockPos pos) {
    if (world == null || pos == null || world.isSkyVisible(pos)) {
      return 0.0f;
    }

    int y = pos.getY();

    if (y >= CAVE_FADE_TOP) {
      return 0.0f;
    }

    if (y <= CAVE_FADE_BOTTOM) {
      return 1.0f;
    }

    return (float) (CAVE_FADE_TOP - y) / (CAVE_FADE_TOP - CAVE_FADE_BOTTOM);
  }

  /**
   * Drops every cached lookup. Must run whenever the client joins or leaves a world.
   *
   * <p>Biome tags arrive from the server as part of the join handshake. A family resolved before
   * they land falls back to DEFAULT, and because the result is memoised per registry entry that
   * wrong answer would stick for the rest of the session, leaving the world with vanilla fog until
   * something happened to replace the entries.
   */
  public static void clearCaches() {
    cachedAggregateCellKey = Long.MIN_VALUE;
    cachedAggregate = null;
    BiomeAtmosphere.clearCache();
    FogTransition.reset();
  }

  private static BiomeAggregate aggregateFor(WorldView world, BlockPos pos) {
    if (world == null || pos == null) {
      return new BiomeAggregate(0xFFFFFF, 0.0f, 1.0f, 0.0f, 20.0f, 90.0f);
    }

    long cellKey = cacheCellKey(pos);

    if (cellKey == cachedAggregateCellKey && cachedAggregate != null) {
      return cachedAggregate;
    }

    BiomeAggregate aggregate = blendAggregate(world, pos);

    cachedAggregateCellKey = cellKey;
    cachedAggregate = aggregate;

    return aggregate;
  }

  /**
   * Averages the biome profile over the sample grid. Colours average per channel and the numeric
   * values average directly, so crossing a biome border ramps rather than snapping.
   */
  private static BiomeAggregate blendAggregate(WorldView world, BlockPos pos) {
    BlockPos.Mutable samplePos = new BlockPos.Mutable();

    float totalRed = 0.0f;
    float totalGreen = 0.0f;
    float totalBlue = 0.0f;
    float totalBlend = 0.0f;
    float totalStrength = 0.0f;
    float totalPresence = 0.0f;
    float totalFogStart = 0.0f;
    float totalFogEnd = 0.0f;

    for (int offsetX : SAMPLE_OFFSETS) {
      for (int offsetZ : SAMPLE_OFFSETS) {
        samplePos.set(pos.getX() + offsetX, pos.getY(), pos.getZ() + offsetZ);

        RegistryEntry<Biome> entry = world.getBiome(samplePos);
        BiomeAtmosphere family = BiomeAtmosphere.of(entry);

        int tint = family.getTintColor();
        totalRed += (tint >> 16) & 0xFF;
        totalGreen += (tint >> 8) & 0xFF;
        totalBlue += tint & 0xFF;

        totalBlend += family.getColorBlend();
        totalStrength += family.getSeasonSensitivity() * strengthForTemperature(entry.value().getTemperature());
        totalPresence += family.getFogPresence();
        totalFogStart += family.getFogStart();
        totalFogEnd += family.getFogEnd();
      }
    }

    int samples = SAMPLE_OFFSETS.length * SAMPLE_OFFSETS.length;

    int tintColor = (Math.round(totalRed / samples) << 16)
            | (Math.round(totalGreen / samples) << 8)
            | Math.round(totalBlue / samples);

    return new BiomeAggregate(
            tintColor,
            totalBlend / samples,
            totalStrength / samples,
            totalPresence / samples,
            totalFogStart / samples,
            totalFogEnd / samples
    );
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

    Grade current = grade();

    float hueShift = current.hueShift() * strength;
    float saturationFactor = applyStrength(current.saturationFactor(), strength);
    float brightnessFactor = applyStrength(current.brightnessFactor(), strength);

    float[] hsb = HSB_SCRATCH.get();
    ColorMath.rgbToHsb(rgb, hsb);

    return ColorMath.hsbToRgb(
            hsb[0] + hueShift,
            hsb[1] * saturationFactor,
            hsb[2] * brightnessFactor
    );
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
