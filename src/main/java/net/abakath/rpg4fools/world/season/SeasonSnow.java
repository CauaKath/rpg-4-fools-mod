package net.abakath.rpg4fools.world.season;

import net.abakath.rpg4fools.enums.SubSeason;

/**
 * The temperature below which it snows, for the season currently in effect.
 *
 * <p>Read from {@code Biome.getPrecipitation}, which runs on the server for every chunk tick and on
 * the client for weather rendering, so the lookup is deliberately a single volatile read and one
 * float comparison.
 *
 * <p>The season itself lives in {@link CurrentSeason}; this class only answers what that season
 * does to snow.
 */
public final class SeasonSnow {
  /**
   * Vanilla snows below this temperature. Used outside the cold months so the mixin never overrides
   * anything: if vanilla already returned RAIN the biome is warmer than this, and the check fails.
   */
  private static final float VANILLA_SNOW_TEMPERATURE = 0.15f;

  private SeasonSnow() {
  }

  /**
   * Biomes at or below this temperature snow right now.
   *
   * <p>The line climbs as winter deepens and pulls back afterwards, so snow spreads out from the
   * already cold biomes and retreats rather than switching on across the map. Jungle sits at 0.95
   * and is deliberately above every value here.
   */
  public static float snowLineTemperature() {
    return switch (CurrentSeason.get()) {
      case LATE_AUTUMN -> 0.30f;
      case EARLY_WINTER -> 0.60f;
      case MID_WINTER -> 0.90f;
      case LATE_WINTER -> 0.70f;
      default -> VANILLA_SNOW_TEMPERATURE;
    };
  }

  /** True when the season is pushing snow past its vanilla range. */
  public static boolean isSnowSeason() {
    return snowLineTemperature() > VANILLA_SNOW_TEMPERATURE;
  }

  /**
   * Biomes at or below this never thaw, whatever the season. Snowy taiga sits at -0.5 and the peaks
   * at -0.7, so those stay frozen year round, while snowy plains and snowy beach sit at 0.0 and
   * above and can lose their cover at the height of summer.
   */
  private static final float PERMAFROST_TEMPERATURE = -0.15f;

  /**
   * Whether snow lying at a biome of this temperature should be melting right now.
   *
   * <p>Snowy biomes are supposed to be white. Their precipitation never stops being snow, so the
   * ordinary rule below already leaves them alone all year, which is what keeps their existing
   * cover intact through the season cycle.
   *
   * <p>Midsummer is the one exception, and only for the milder of them. A snowy plains losing its
   * cover for a month reads as a thaw; frozen peaks doing the same would just look broken.
   */
  public static boolean shouldThaw(float biomeTemperature) {
    if (CurrentSeason.get() == SubSeason.MID_SUMMER) {
      return biomeTemperature > PERMAFROST_TEMPERATURE;
    }

    // Anything at or below the current snow line is actively being snowed on, so it is not thawing.
    // For a snowy biome that condition holds in every season, which is the year round protection.
    return biomeTemperature > snowLineTemperature();
  }

  /**
   * True when the thaw is the midsummer exception rather than the ordinary end of winter.
   *
   * <p>The caller uses this to leave ice alone: clearing a snowy plains of its snow for a month is
   * the intent, draining a frozen ocean is not.
   */
  public static boolean isMidsummerThaw() {
    return CurrentSeason.get() == SubSeason.MID_SUMMER;
  }
}
