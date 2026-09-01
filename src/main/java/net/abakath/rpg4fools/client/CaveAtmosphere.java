package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Atmosphere for underground space.
 *
 * <p>Caves are a place rather than a biome family, so they are matched separately and take over
 * from {@link BiomeAtmosphere} entirely once the player is deep enough with no sky above. Mixing
 * the two would let surface weather bleed into a cave.
 *
 * <p>Season sensitivity is zero throughout on purpose: the calendar should not reach underground.
 * These are matched by biome key rather than by tag, because the cave biomes are a small closed set
 * with no conventional tags covering them.
 */
@Environment(EnvType.CLIENT)
public enum CaveAtmosphere {
  DRIPSTONE(0x6B5B4A, 0.55f, 0.95f, 4.0f, 24.0f),
  LUSH(0x4A7A3A, 0.50f, 0.95f, 5.0f, 28.0f),
  DEEP_DARK(0x0E1418, 0.75f, 1.00f, 1.0f, 12.0f),
  GENERIC(0x2A2E36, 0.45f, 0.95f, 3.0f, 20.0f);

  private final int tintColor;
  private final float colorBlend;
  private final float fogPresence;
  private final float fogStart;
  private final float fogEnd;

  CaveAtmosphere(int tintColor, float colorBlend, float fogPresence, float fogStart, float fogEnd) {
    this.tintColor = tintColor;
    this.colorBlend = colorBlend;
    this.fogPresence = fogPresence;
    this.fogStart = fogStart;
    this.fogEnd = fogEnd;
  }

  public int getTintColor() {
    return tintColor;
  }

  public float getColorBlend() {
    return colorBlend;
  }

  public float getFogPresence() {
    return fogPresence;
  }

  /** Distance in blocks where the fog begins. */
  public float getFogStart() {
    return fogStart;
  }

  /** Distance in blocks where the fog is opaque. */
  public float getFogEnd() {
    return fogEnd;
  }

  public static CaveAtmosphere of(Holder<Biome> entry) {
    if (entry == null) {
      return GENERIC;
    }

    if (entry.is(Biomes.DRIPSTONE_CAVES)) {
      return DRIPSTONE;
    }

    if (entry.is(Biomes.LUSH_CAVES)) {
      return LUSH;
    }

    if (entry.is(Biomes.DEEP_DARK)) {
      return DEEP_DARK;
    }

    return GENERIC;
  }
}
