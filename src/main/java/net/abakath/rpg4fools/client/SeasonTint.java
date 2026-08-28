package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.SubSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Per sub season colour grade applied on top of the biome colour.
 *
 * <p>Saturation alone cannot express a season. Autumn has to walk the hue from green towards
 * orange and brown, and winter has to drop saturation while lifting brightness so it reads as pale
 * frost rather than dark olive. So each sub season carries a hue shift, a saturation factor and a
 * brightness factor.
 *
 * <p>Hue shifts are stored in degrees for readability and converted to the 0..1 hue space used by
 * {@link ColorMath}. Grass sits around hue 88 degrees, so a negative shift walks green to yellow to
 * orange to brown.
 *
 * <p>Shifts stay close between neighbouring sub seasons on purpose. The values are interpolated
 * linearly across the month, so a large jump would sweep the hue through every colour in between.
 * Winter therefore reads as pale frost through low saturation and high brightness rather than
 * through a swing towards blue, which would have to travel back across green to get there.
 */
@Environment(EnvType.CLIENT)
public enum SeasonTint {
  EARLY_SPRING(SubSeason.EARLY_SPRING, 5.0f, 0.85f, 1.05f),
  MID_SPRING(SubSeason.MID_SPRING, 0.0f, 1.05f, 1.05f),
  LATE_SPRING(SubSeason.LATE_SPRING, 0.0f, 1.15f, 1.0f),
  EARLY_SUMMER(SubSeason.EARLY_SUMMER, -3.0f, 1.2f, 1.0f),
  MID_SUMMER(SubSeason.MID_SUMMER, -5.0f, 1.3f, 0.95f),
  LATE_SUMMER(SubSeason.LATE_SUMMER, -12.0f, 1.15f, 0.95f),
  EARLY_AUTUMN(SubSeason.EARLY_AUTUMN, -30.0f, 1.1f, 1.0f),
  MID_AUTUMN(SubSeason.MID_AUTUMN, -50.0f, 1.15f, 0.95f),
  LATE_AUTUMN(SubSeason.LATE_AUTUMN, -62.0f, 0.9f, 0.85f),
  EARLY_WINTER(SubSeason.EARLY_WINTER, -70.0f, 0.45f, 0.85f),
  MID_WINTER(SubSeason.MID_WINTER, -75.0f, 0.15f, 1.15f),
  LATE_WINTER(SubSeason.LATE_WINTER, -40.0f, 0.30f, 1.05f);

  private static final float DEGREES_IN_CIRCLE = 360.0f;

  private final SubSeason subSeason;
  private final float hueShiftDegrees;
  private final float saturationFactor;
  private final float brightnessFactor;

  SeasonTint(SubSeason subSeason, float hueShiftDegrees, float saturationFactor, float brightnessFactor) {
    this.subSeason = subSeason;
    this.hueShiftDegrees = hueShiftDegrees;
    this.saturationFactor = saturationFactor;
    this.brightnessFactor = brightnessFactor;
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

  public static SeasonTint of(SubSeason subSeason) {
    return values()[subSeason.ordinal()];
  }

  /**
   * The tint that follows this one in the yearly cycle, wrapping from LATE_WINTER to EARLY_SPRING.
   */
  public SeasonTint next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
