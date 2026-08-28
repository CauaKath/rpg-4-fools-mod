package net.abakath.rpg4fools.world;

import net.abakath.rpg4fools.enums.SubSeason;

/**
 * The temperature below which it snows, for the season currently in effect.
 *
 * <p>Read from {@code Biome.getPrecipitation}, which runs on the server for every chunk tick and on
 * the client for weather rendering, so the lookup is deliberately a single volatile read and one
 * float comparison.
 *
 * <p>Held statically rather than passed around because both sides need it and neither has a handle
 * on the other's season state: the server owns SeasonData, the client owns ClientSeasonState. Both
 * write here. In single player they share a JVM and write the same value, which is harmless.
 */
public final class SeasonSnow {
  /**
   * Vanilla snows below this temperature. Used outside the cold months so the mixin never overrides
   * anything: if vanilla already returned RAIN the biome is warmer than this, and the check fails.
   */
  private static final float VANILLA_SNOW_TEMPERATURE = 0.15f;

  private static volatile SubSeason subSeason = SubSeason.EARLY_SPRING;

  private SeasonSnow() {
  }

  public static void setSubSeason(SubSeason current) {
    if (current != null) {
      subSeason = current;
    }
  }

  public static SubSeason getSubSeason() {
    return subSeason;
  }

  /**
   * Biomes at or below this temperature snow right now.
   *
   * <p>The line climbs as winter deepens and pulls back afterwards, so snow spreads out from the
   * already cold biomes and retreats rather than switching on across the map. Jungle sits at 0.95
   * and is deliberately above every value here.
   */
  public static float snowLineTemperature() {
    return switch (subSeason) {
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
}
