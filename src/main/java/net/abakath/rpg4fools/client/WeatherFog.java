package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * How much the current weather thickens the fog.
 *
 * <p>Snow counts for more than rain. Snow falling through already freezing air is the case the mod
 * is trying to sell, so a snowy biome mid blizzard is the densest the mist ever gets.
 *
 * <p>Weather is read from the client world, which tracks rain and thunder gradients rather than a
 * boolean, so the mist thickens as the storm rolls in instead of switching on.
 */
@Environment(EnvType.CLIENT)
public final class WeatherFog {
  /** Multiplier at full rain, for biomes where the precipitation falls as water. */
  private static final float RAIN_MULTIPLIER = 1.30f;

  /** Multiplier at full snowfall. Higher than rain: this is the effect worth selling. */
  private static final float SNOW_MULTIPLIER = 1.60f;

  /** Extra on top during a thunderstorm. */
  private static final float THUNDER_MULTIPLIER = 1.15f;

  private WeatherFog() {
  }

  /**
   * Multiplier to apply to the fog presence, 1 in clear weather.
   *
   * @param rainGradient    0 to 1, how far into rain or snow the sky is
   * @param thunderGradient 0 to 1, how far into a thunderstorm
   * @param snowing         whether precipitation falls as snow at this position
   */
  public static float multiplier(float rainGradient, float thunderGradient, boolean snowing) {
    float peak = snowing ? SNOW_MULTIPLIER : RAIN_MULTIPLIER;

    float weather = 1.0f + (peak - 1.0f) * ColorMath.clamp01(rainGradient);
    float thunder = 1.0f + (THUNDER_MULTIPLIER - 1.0f) * ColorMath.clamp01(thunderGradient);

    return weather * thunder;
  }

  /**
   * Reads the weather at a position and returns the fog multiplier for it.
   *
   * <p>Precipitation type comes from the biome rather than from the world, since whether it snows
   * is a property of where you are standing, not of the sky.
   */
  public static float multiplierAt(World world, BlockPos pos) {
    if (world == null || pos == null) {
      return 1.0f;
    }

    float rainGradient = world.getRainGradient(1.0f);

    if (rainGradient <= 0.0f) {
      return 1.0f;
    }

    // Weather cannot reach through a roof, so a cave or a house should not thicken. This also keeps
    // the cave mist, which is meant to ignore the sky entirely, out of the weather path.
    if (!world.isSkyVisible(pos)) {
      return 1.0f;
    }

    Biome biome = world.getBiome(pos).value();
    boolean snowing = biome.getPrecipitation(pos) == Biome.Precipitation.SNOW;

    return multiplier(rainGradient, world.getThunderGradient(1.0f), snowing);
  }
}
