package net.abakath.rpg4fools.client.atmosphere;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Eases the fog distances towards wherever the atmosphere currently points.
 *
 * <p>What this smooths is not the biome blend - 26.2 interpolates that itself, between ticks and
 * across neighbouring biomes. It is everything the mod decides on top of that, which moves in
 * steps: the aggregate is cached per four block cell, sky occlusion is counted in ninths, and the
 * cave factor rides a Y band. Each of those is a jump on its own, and together they read as the fog
 * changing all at once.
 *
 * <p>Advanced from exactly one place, once a frame, with the frame's own delta. An earlier version
 * of this kept wall clock time and was advanced from an attribute lookup instead, which is asked
 * many times a frame and by more than one probe - so it integrated the same frame repeatedly and
 * the value wandered instead of settling. The rate still comes from elapsed time rather than being
 * applied per frame, so the transition takes the same wall clock duration at 30 fps and at 240.
 */
@Environment(EnvType.CLIENT)
public final class FogEasing {
  /**
   * Ticks for the fog to cover about 63 percent of the distance to a new target. Sixteen is about
   * eight tenths of a second: long enough that a canopy edge reads as walking out of cover, short
   * enough that it does not lag behind a player sprinting across one.
   */
  private static final float TIME_CONSTANT_TICKS = 16.0f;

  /** Beyond this gap the value is snapped rather than eased, so a teleport does not crawl. */
  private static final float SNAP_THRESHOLD = 400.0f;

  /** Longest step to integrate at once, so a stall does not jump the easing. */
  private static final float MAX_STEP_TICKS = 5.0f;

  private static float currentStart = Float.NaN;
  private static float currentEnd = Float.NaN;

  private FogEasing() {
  }

  /**
   * Moves the eased pair towards the given targets.
   *
   * <p>Call once per frame, before {@link #start()} and {@link #end()}.
   *
   * @param deltaTicks real time elapsed since the last frame, in ticks
   */
  public static void advance(float targetStart, float targetEnd, float deltaTicks) {
    if (Float.isNaN(currentStart) || Float.isNaN(currentEnd)) {
      currentStart = targetStart;
      currentEnd = targetEnd;

      return;
    }

    float elapsed = Math.min(MAX_STEP_TICKS, Math.max(0.0f, deltaTicks));

    if (elapsed <= 0.0f) {
      return;
    }

    // Exponential approach. Framerate independent because the factor comes from elapsed time.
    float factor = 1.0f - (float) Math.exp(-elapsed / TIME_CONSTANT_TICKS);

    currentStart = approach(currentStart, targetStart, factor);
    currentEnd = approach(currentEnd, targetEnd, factor);
  }

  public static float start() {
    return currentStart;
  }

  public static float end() {
    return currentEnd;
  }

  /** Forgets the eased state, so the next frame snaps instead of sliding in from a stale value. */
  public static void reset() {
    currentStart = Float.NaN;
    currentEnd = Float.NaN;
  }

  private static float approach(float current, float target, float factor) {
    if (Math.abs(target - current) > SNAP_THRESHOLD) {
      return target;
    }

    return current + (target - current) * factor;
  }
}
