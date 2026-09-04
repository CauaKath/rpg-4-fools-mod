package net.abakath.rpg4fools.client.atmosphere;

import net.abakath.rpg4fools.client.season.SeasonTint;
import net.abakath.rpg4fools.enums.SubSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Per sub season grade for the sky colour and the fog distance.
 *
 * <p>Kept separate from {@link SeasonTint}, which grades block colours. Blocks and sky want
 * different curves: foliage swings hard towards orange in autumn, while the sky mostly cools and
 * pales. Sharing one table would force a compromise on both.
 *
 * <p>Hue shifts are in degrees and converted to the 0..1 space used by {@link ColorMath}. As with
 * SeasonTint, neighbouring values are kept close on purpose, because the grade is interpolated
 * linearly across the month and a large jump would sweep the hue through every colour in between.
 *
 * it in, above 1 pushes it out.
 *
 * the cold months on purpose: vanilla starts its fog far out, so scaling both by the same amount
 * still reads as a distant band on the horizon rather than haze in the air around the player.
 */
@Environment(EnvType.CLIENT)
public enum AtmosphereTint {
  EARLY_SPRING(SubSeason.EARLY_SPRING, 2.0f, 0.90f, 1.02f, 0.10f),
  MID_SPRING(SubSeason.MID_SPRING, 0.0f, 0.98f, 1.04f, 0.02f),
  LATE_SPRING(SubSeason.LATE_SPRING, -2.0f, 1.02f, 1.04f, 0.00f),
  EARLY_SUMMER(SubSeason.EARLY_SUMMER, -4.0f, 1.06f, 1.03f, 0.00f),
  MID_SUMMER(SubSeason.MID_SUMMER, -6.0f, 1.10f, 1.02f, 0.00f),
  LATE_SUMMER(SubSeason.LATE_SUMMER, -8.0f, 1.06f, 1.00f, 0.00f),
  EARLY_AUTUMN(SubSeason.EARLY_AUTUMN, -12.0f, 1.00f, 0.98f, 0.05f),
  MID_AUTUMN(SubSeason.MID_AUTUMN, -16.0f, 0.94f, 0.95f, 0.15f),
  LATE_AUTUMN(SubSeason.LATE_AUTUMN, -14.0f, 0.82f, 0.92f, 0.28f),
  EARLY_WINTER(SubSeason.EARLY_WINTER, -8.0f, 0.66f, 0.92f, 0.40f),
  MID_WINTER(SubSeason.MID_WINTER, -2.0f, 0.55f, 0.95f, 0.45f),
  LATE_WINTER(SubSeason.LATE_WINTER, 0.0f, 0.70f, 0.98f, 0.32f);

  private static final float DEGREES_IN_CIRCLE = 360.0f;

  private final SubSeason subSeason;
  private final float hueShiftDegrees;
  private final float saturationFactor;
  private final float brightnessFactor;
  private final float fogPresenceBoost;

  AtmosphereTint(SubSeason subSeason,
                 float hueShiftDegrees,
                 float saturationFactor,
                 float brightnessFactor,
                 float fogPresenceBoost) {
    this.subSeason = subSeason;
    this.hueShiftDegrees = hueShiftDegrees;
    this.saturationFactor = saturationFactor;
    this.brightnessFactor = brightnessFactor;
    this.fogPresenceBoost = fogPresenceBoost;
  }

  public SubSeason getSubSeason() {
    return subSeason;
  }

  public float getHueShift() {
    return hueShiftDegrees / DEGREES_IN_CIRCLE;
  }

  public float getSaturationFactor() {
    return saturationFactor;
  }

  public float getBrightnessFactor() {
    return brightnessFactor;
  }

  /**
   * Fog presence this sub season adds on top of a family's baseline, rather than multiplies. That
   * is what lets an open biome sitting at zero baseline still get thin cold fog in winter while
   * staying completely clear in summer.
   */
  public float getFogPresenceBoost() {
    return fogPresenceBoost;
  }

  public static AtmosphereTint of(SubSeason subSeason) {
    return values()[subSeason.ordinal()];
  }

  /** The tint that follows this one, wrapping from LATE_WINTER back to EARLY_SPRING. */
  public AtmosphereTint next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
