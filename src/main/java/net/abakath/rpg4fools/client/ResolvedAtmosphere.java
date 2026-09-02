package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The finished atmosphere at a position: what colour to pull the fog towards, and how close the fog
 * should close in around the player.
 *
 * <p>Fog distances are absolute, in blocks, rather than factors on the view distance. A factor
 * means the same biome feels completely different at render distance 8 and render distance 32,
 * and it also cannot express "you can see 24 blocks in a swamp" at all. The presence value decides
 * how far to move from vanilla's own distances towards the biome's target, so presence zero returns
 * vanilla exactly.
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

  /**
   * Distance in blocks where the fog should be fully opaque.
   *
   * @param vanillaEnd where vanilla put its own fog end this frame
   */
  public float fogEnd(float vanillaEnd) {
    float blended = lerp(vanillaEnd, fogTargetEnd, fogPresence);

    // Never push the fog further out than vanilla would. At a low render distance the biome target
    // can be the larger of the two, and thinning the fog is not what any of these families mean.
    return Math.min(vanillaEnd, blended);
  }

  /**
   * Distance in blocks where the fog should begin.
   *
   * <p>Derived as a ratio of where the fog ends rather than interpolated on its own. Interpolating
   * both ends independently let the start lag far behind: a swamp reaching an end of 41 blocks
   * still began at 16, which reads as a wall ahead rather than as thick air around you.
   *
   * <p>Both ratios are measured against vanilla's actual values rather than assumed, so at zero
   * presence this returns exactly what vanilla asked for and a biome with no fog of its own is left
   * untouched.
   *
   * @param vanillaStart where vanilla put its own fog start this frame
   * @param vanillaEnd   where vanilla put its own fog end this frame
   */
  public float fogStart(float vanillaStart, float vanillaEnd) {
    if (vanillaEnd <= 0.0f || fogTargetEnd <= 0.0f) {
      return vanillaStart;
    }

    float vanillaRatio = vanillaStart / vanillaEnd;
    float targetRatio = fogTargetStart / fogTargetEnd;
    float ratio = lerp(vanillaRatio, targetRatio, fogPresence);

    return fogEnd(vanillaEnd) * ratio;
  }

  /** Applies the biome tint and then the season grade to a packed fog colour. */
  public int applyToFogColor(int vanillaRgb) {
    int tinted = ColorMath.lerpRgb(vanillaRgb, tintColor, colorBlend);

    return ColorMath.opaque(SeasonAtmosphere.gradeSkyColor(tinted, seasonStrength));
  }

  /** Applies a weakened biome tint and then the season grade to a packed sky colour. */
  public int applyToSkyColor(int vanillaRgb) {
    int tinted = ColorMath.lerpRgb(vanillaRgb, tintColor, colorBlend * SKY_BLEND_SCALE);

    return ColorMath.opaque(SeasonAtmosphere.gradeSkyColor(tinted, seasonStrength));
  }

  private static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }
}
