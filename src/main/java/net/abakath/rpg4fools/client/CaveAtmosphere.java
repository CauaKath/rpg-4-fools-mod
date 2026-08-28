package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

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
  DRIPSTONE(0x6B5B4A, 0.55f, 0.62f, 0.50f),
  LUSH(0x4A7A3A, 0.50f, 0.68f, 0.55f),
  DEEP_DARK(0x0E1418, 0.75f, 0.42f, 0.30f),
  GENERIC(0x2A2E36, 0.45f, 0.72f, 0.60f);

  private final int tintColor;
  private final float colorBlend;
  private final float density;
  private final float startFactor;

  CaveAtmosphere(int tintColor, float colorBlend, float density, float startFactor) {
    this.tintColor = tintColor;
    this.colorBlend = colorBlend;
    this.density = density;
    this.startFactor = startFactor;
  }

  public int getTintColor() {
    return tintColor;
  }

  public float getColorBlend() {
    return colorBlend;
  }

  public float getDensity() {
    return density;
  }

  /** Factor on the vanilla fog start. Lower brings the mist right up to the player. */
  public float getStartFactor() {
    return startFactor;
  }

  public static CaveAtmosphere of(RegistryEntry<Biome> entry) {
    if (entry == null) {
      return GENERIC;
    }

    if (entry.matchesKey(BiomeKeys.DRIPSTONE_CAVES)) {
      return DRIPSTONE;
    }

    if (entry.matchesKey(BiomeKeys.LUSH_CAVES)) {
      return LUSH;
    }

    if (entry.matchesKey(BiomeKeys.DEEP_DARK)) {
      return DEEP_DARK;
    }

    return GENERIC;
  }
}
