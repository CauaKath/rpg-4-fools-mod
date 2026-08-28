package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

/**
 * The finished atmosphere at a position: what colour to pull the fog towards, how far to pull it,
 * and where the fog starts and ends.
 *
 * <p>This is the only atmosphere type the mixins see, and every multiplication that produces these
 * numbers happens in {@link SeasonAtmosphere#resolve}. Three factors compose here (a family's base
 * density, that family's season sensitivity, and the biome temperature curve), and spreading that
 * arithmetic across four call sites is how a surprising result becomes hard to trace back.
 *
 * @param tintColor          packed 0xRRGGBB colour the vanilla fog is pulled towards
 * @param colorBlend         how far to pull towards tintColor, 0 to 1
 * @param fogDistanceFactor  factor on the vanilla view distance, which governs where fog ends
 * @param fogStartFactor     factor on the vanilla fog start, already compensated for the distance
 *                           factor the density hook applies
 * @param seasonStrength     how strongly the season applies here, used for the colour grade
 * @param mistDensity        how much suspended mist to draw here, 0 to 1
 */
@Environment(EnvType.CLIENT)
public record ResolvedAtmosphere(
        int tintColor,
        float colorBlend,
        float fogDistanceFactor,
        float fogStartFactor,
        float seasonStrength,
        float mistDensity
) {
  /**
   * The sky takes the biome tint at a reduced weight. A swamp should feel humid and green near the
   * ground without turning the whole sky green, so the biome reads mostly in the fog.
   */
  private static final float SKY_BLEND_SCALE = 0.35f;

  /** Applies the biome tint and then the season grade to a packed fog colour. */
  public int applyToFogColor(int vanillaRgb) {
    int tinted = ColorMath.lerpRgb(vanillaRgb, tintColor, colorBlend);

    return SeasonAtmosphere.gradeSkyColor(tinted, seasonStrength);
  }

  /** Applies a weakened biome tint and then the season grade to a sky colour. */
  public Vec3d applyToSkyColor(Vec3d vanillaColor) {
    int packed = ColorMath.packRgb(vanillaColor);
    int tinted = ColorMath.lerpRgb(packed, tintColor, colorBlend * SKY_BLEND_SCALE);

    return ColorMath.unpackRgb(SeasonAtmosphere.gradeSkyColor(tinted, seasonStrength));
  }
}
