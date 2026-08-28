package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Eases the fog distances towards wherever the biome blend currently points.
 *
 * <p>The spatial blend samples a grid of biomes and averages them, which moves in steps: every
 * sample that crosses a border flips at once, and with distances as far apart as a swamp's 24
 * blocks and a forest's 146 a single step is a large visible jump. Widening the kernel spreads
 * those steps out but cannot remove them.
 *
 * <p>Easing over time removes them instead. Whatever the blend reports, the value actually handed
 * to the renderer walks towards it, so a step in the source becomes a slide on screen.
 *
 * <p>The rate is derived from elapsed real time rather than applied per frame, so the transition
 * takes the same wall clock duration at 30 fps and at 240 fps.
 */
@Environment(EnvType.CLIENT)
public final class FogTransition {
  /**
   * Seconds for the fog to cover about 63 percent of the distance to a new target. Long enough that
   * a biome border reads as walking into the fog, short enough that it does not lag behind a player
   * sprinting across one.
   */
  private static final float TIME_CONSTANT_SECONDS = 0.8f;

  /** Beyond this gap the value is snapped rather than eased, so a teleport does not crawl. */
  private static final float SNAP_THRESHOLD = 400.0f;

  private static final float NANOS_PER_SECOND = 1_000_000_000.0f;

  /** Longest step to integrate at once, so a stall does not jump the easing. */
  private static final float MAX_STEP_SECONDS = 0.25f;

  private static float currentStart = Float.NaN;
  private static float currentEnd = Float.NaN;
  private static long lastUpdateNanos = 0L;

  private FogTransition() {
  }

  /**
   * Advances the eased values towards the given targets and returns the smoothed start.
   *
   * <p>Call once per frame, before {@link #getEnd()}.
   */
  public static float update(float targetStart, float targetEnd, long nowNanos) {
    if (Float.isNaN(currentStart) || Float.isNaN(currentEnd)) {
      currentStart = targetStart;
      currentEnd = targetEnd;
      lastUpdateNanos = nowNanos;

      return currentStart;
    }

    float elapsed = Math.min(MAX_STEP_SECONDS, (nowNanos - lastUpdateNanos) / NANOS_PER_SECOND);
    lastUpdateNanos = nowNanos;

    if (elapsed <= 0.0f) {
      return currentStart;
    }

    // Exponential approach. Framerate independent because the factor comes from elapsed time.
    float factor = 1.0f - (float) Math.exp(-elapsed / TIME_CONSTANT_SECONDS);

    currentStart = approach(currentStart, targetStart, factor);
    currentEnd = approach(currentEnd, targetEnd, factor);

    return currentStart;
  }

  public static float getEnd() {
    return currentEnd;
  }

  private static float approach(float current, float target, float factor) {
    if (Math.abs(target - current) > SNAP_THRESHOLD) {
      return target;
    }

    return current + (target - current) * factor;
  }

  /** Forgets the eased state, so the next frame snaps instead of sliding in from a stale value. */
  public static void reset() {
    currentStart = Float.NaN;
    currentEnd = Float.NaN;
    lastUpdateNanos = 0L;
  }
}
