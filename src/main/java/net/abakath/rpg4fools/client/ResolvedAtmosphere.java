package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

/**
 * The finished atmosphere at a position: what colour to pull the fog towards, and how close the fog
 * should close in around the player.
 *
 * <p>Fog distances are absolute, in blocks, rather than factors on the view distance. A factor
 * means the same biome feels completely different at render distance 8 and render distance 32,
 * and it also cannot express "you can see 24 blocks in a swamp" at all. The presence value decides
 * how far to move from the vanilla distance towards the biome's target.
 *
 * <p>Every multiplication that produces these numbers happens here or in
 * {@link SeasonAtmosphere#resolve}, never in a mixin.
 *
 * @param tintColor      packed 0xRRGGBB colour the vanilla fog is pulled towards
 * @param colorBlend     how far to pull towards tintColor, 0 to 1
 * @param seasonStrength how strongly the season applies here, used for the colour grade
 * @param fogPresence    how far to move from vanilla fog towards this biome's target, 0 to 1
 * @param fogTargetStart distance in blocks the fog begins at when presence is 1
 * @param fogTargetEnd   distance in blocks the fog is opaque at when presence is 1
 */
@Environment(EnvType.CLIENT)
public record ResolvedAtmosphere(
        int tintColor,
        float colorBlend,
        float seasonStrength,
        float fogPresence,
        float fogTargetStart,
        float fogTargetEnd
) {
  /**
   * The sky takes the biome tint at a reduced weight. A swamp should feel humid and green near the
   * ground without turning the whole sky green, so the biome reads mostly in the fog.
   */
  private static final float SKY_BLEND_SCALE = 0.35f;

  /** Where vanilla starts its terrain fog, as a fraction of where it ends. */
  private static final float VANILLA_START_RATIO = 0.75f;

  /** Distance in blocks where the fog should be fully opaque, given vanilla's view distance. */
  public float fogEnd(float viewDistance) {
    float blended = lerp(viewDistance, fogTargetEnd, fogPresence);

    // Never push the fog further out than vanilla would. At a low render distance the biome target
    // can be the larger of the two, and thinning the fog is not what any of these families mean.
    return Math.min(viewDistance, blended);
  }

  /**
   * Distance in blocks where the fog should begin, given vanilla's view distance.
   *
   * <p>Derived as a ratio of where the fog ends rather than interpolated from vanilla's own start.
   * Interpolating both ends independently let the start lag far behind: a swamp reaching an end of
   * 41 blocks still began at 16, which reads as a wall ahead rather than as thick air around you.
   * Tying the ratio to the biome keeps the gradient proportional at any density.
   */
  public float fogStart(float viewDistance) {
    float targetRatio = fogTargetEnd <= 0.0f ? VANILLA_START_RATIO : fogTargetStart / fogTargetEnd;
    float ratio = lerp(VANILLA_START_RATIO, targetRatio, fogPresence);

    return fogEnd(viewDistance) * ratio;
  }

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

  private static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }
}
