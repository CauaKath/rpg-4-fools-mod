package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;

/**
 * Allocation free HSB helpers for packed 0xRRGGBB ints.
 *
 * <p>These run once per tinted quad on every chunk rebuild, so the whole point is that they touch
 * no objects. {@link java.awt.Color} was previously used here and allocated a Color plus a float[3]
 * per call, which adds up to millions of allocations per world reload. AWT is also an awkward
 * dependency to pull into a Minecraft client.
 */
@Environment(EnvType.CLIENT)
public final class ColorMath {
  private ColorMath() {
  }

  /**
   * Converts a packed RGB colour to HSB and writes the result into {@code out}.
   *
   * @param out a caller owned float[3] receiving hue, saturation and brightness, each in 0..1
   */
  public static void rgbToHsb(int rgb, float[] out) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;

    int max = Math.max(r, Math.max(g, b));
    int min = Math.min(r, Math.min(g, b));
    int delta = max - min;

    float brightness = max / 255.0f;
    float saturation = max == 0 ? 0.0f : (float) delta / max;
    float hue = 0.0f;

    if (delta != 0) {
      float rc = (float) (max - r) / delta;
      float gc = (float) (max - g) / delta;
      float bc = (float) (max - b) / delta;

      if (r == max) {
        hue = bc - gc;
      } else if (g == max) {
        hue = 2.0f + rc - bc;
      } else {
        hue = 4.0f + gc - rc;
      }

      hue /= 6.0f;
      if (hue < 0.0f) {
        hue += 1.0f;
      }
    }

    out[0] = hue;
    out[1] = saturation;
    out[2] = brightness;
  }

  /** Inputs are clamped, and hue wraps. */
  public static int hsbToRgb(float hue, float saturation, float brightness) {
    saturation = clamp01(saturation);
    brightness = clamp01(brightness);

    hue = hue - (float) Math.floor(hue);

    int r = 0;
    int g = 0;
    int b = 0;

    if (saturation == 0.0f) {
      r = g = b = Math.round(brightness * 255.0f);
    } else {
      float h = hue * 6.0f;
      int sector = (int) Math.floor(h);
      float f = h - sector;

      float p = brightness * (1.0f - saturation);
      float q = brightness * (1.0f - saturation * f);
      float t = brightness * (1.0f - saturation * (1.0f - f));

      switch (sector) {
        case 0 -> {
          r = Math.round(brightness * 255.0f);
          g = Math.round(t * 255.0f);
          b = Math.round(p * 255.0f);
        }
        case 1 -> {
          r = Math.round(q * 255.0f);
          g = Math.round(brightness * 255.0f);
          b = Math.round(p * 255.0f);
        }
        case 2 -> {
          r = Math.round(p * 255.0f);
          g = Math.round(brightness * 255.0f);
          b = Math.round(t * 255.0f);
        }
        case 3 -> {
          r = Math.round(p * 255.0f);
          g = Math.round(q * 255.0f);
          b = Math.round(brightness * 255.0f);
        }
        case 4 -> {
          r = Math.round(t * 255.0f);
          g = Math.round(p * 255.0f);
          b = Math.round(brightness * 255.0f);
        }
        default -> {
          r = Math.round(brightness * 255.0f);
          g = Math.round(p * 255.0f);
          b = Math.round(q * 255.0f);
        }
      }
    }

    return (r << 16) | (g << 8) | b;
  }

  public static int lerpRgb(int from, int to, float t) {
    float amount = clamp01(t);

    int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
    int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
    int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);

    return (r << 16) | (g << 8) | b;
  }

  public static int packRgb(Vec3 color) {
    int r = Math.round(clamp01((float) color.x) * 255.0f);
    int g = Math.round(clamp01((float) color.y) * 255.0f);
    int b = Math.round(clamp01((float) color.z) * 255.0f);

    return (r << 16) | (g << 8) | b;
  }

  public static Vec3 unpackRgb(int rgb) {
    return new Vec3(
            ((rgb >> 16) & 0xFF) / 255.0,
            ((rgb >> 8) & 0xFF) / 255.0,
            (rgb & 0xFF) / 255.0
    );
  }

  public static float clamp01(float value) {
    return value < 0.0f ? 0.0f : Math.min(value, 1.0f);
  }
}
