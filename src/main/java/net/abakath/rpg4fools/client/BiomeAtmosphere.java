package net.abakath.rpg4fools.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Atmosphere identity for a family of biomes.
 *
 * <p>Each family carries a colour the vanilla fog is pulled towards, how far to pull it, a base
 * density that applies year round, and how strongly the season is allowed to move that density.
 * A swamp is dense whatever the month; a taiga is only dense in winter.
 *
 * <p>Families are matched by Fabric's conventional biome tags rather than by naming biomes
 * directly, so modded biomes that apply the standard tags get a sensible atmosphere for free.
 * Matching runs in declaration order and stops at the first hit, which is why the more specific
 * families come first: a snowy taiga should read as SNOWY, not TAIGA.
 */
@Environment(EnvType.CLIENT)
public enum BiomeAtmosphere {
  SWAMP(ConventionalBiomeTags.IS_SWAMP, 0x5F7A22, 0.55f, 0.60f, 0.20f, 0.90f),
  SNOWY(ConventionalBiomeTags.IS_SNOWY, 0xC8DCFF, 0.45f, 0.75f, 0.90f, 0.85f),
  TAIGA(ConventionalBiomeTags.IS_TAIGA, 0xA8C0D0, 0.35f, 0.85f, 1.00f, 0.50f),
  JUNGLE(ConventionalBiomeTags.IS_JUNGLE, 0x4E8C3A, 0.40f, 0.70f, 0.35f, 0.60f),
  BADLANDS(ConventionalBiomeTags.IS_BADLANDS, 0xC8873F, 0.40f, 0.85f, 0.15f, 0.15f),
  DESERT(ConventionalBiomeTags.IS_DESERT, 0xE0C88A, 0.30f, 1.00f, 0.10f, 0.00f),
  SAVANNA(ConventionalBiomeTags.IS_SAVANNA, 0xC4B87A, 0.25f, 0.95f, 0.25f, 0.00f),
  MOUNTAIN(ConventionalBiomeTags.IS_MOUNTAIN, 0xD0DCE8, 0.30f, 0.90f, 0.80f, 0.30f),
  FOREST(ConventionalBiomeTags.IS_FOREST, 0x8FA870, 0.20f, 0.90f, 0.85f, 0.25f),

  /**
   * Fallback for anything untagged, including plains. Blend of zero means the vanilla fog colour is
   * left exactly as it is, so an unrecognised biome still behaves like it does today.
   */
  DEFAULT(null, 0xFFFFFF, 0.00f, 1.00f, 1.00f, 0.00f);

  /**
   * Memoises the family per biome entry. Tag matching walks every family in the worst case, and the
   * blend samples 25 positions, so without this a single cache miss could run hundreds of tag
   * lookups. Registry entries are stable identities, so reference keys are safe here.
   *
   * <p>Only touched from the render thread.
   */
  private static final Map<RegistryEntry<Biome>, BiomeAtmosphere> CACHE = new IdentityHashMap<>();

  private final TagKey<Biome> tag;
  private final int tintColor;
  private final float colorBlend;
  private final float baseDensity;
  private final float seasonSensitivity;
  private final float mistDensity;

  BiomeAtmosphere(TagKey<Biome> tag,
                  int tintColor,
                  float colorBlend,
                  float baseDensity,
                  float seasonSensitivity,
                  float mistDensity) {
    this.tag = tag;
    this.tintColor = tintColor;
    this.colorBlend = colorBlend;
    this.baseDensity = baseDensity;
    this.seasonSensitivity = seasonSensitivity;
    this.mistDensity = mistDensity;
  }

  /** Colour the vanilla fog colour is pulled towards. */
  public int getTintColor() {
    return tintColor;
  }

  /** How far to pull the vanilla fog colour towards {@link #getTintColor()}, from 0 to 1. */
  public float getColorBlend() {
    return colorBlend;
  }

  /** Density factor that applies regardless of season. Below 1 pulls the fog in. */
  public float getBaseDensity() {
    return baseDensity;
  }

  /** How strongly the season is allowed to move this family's density, from 0 to 1. */
  public float getSeasonSensitivity() {
    return seasonSensitivity;
  }

  /**
   * How much suspended mist this family carries, from 0 to 1, before the season and the weather
   * have their say. Open biomes sit at 0 on purpose: plains should only ever get mist because the
   * season put it there, never as a baseline.
   */
  public float getMistDensity() {
    return mistDensity;
  }

  public static BiomeAtmosphere of(RegistryEntry<Biome> entry) {
    if (entry == null) {
      return DEFAULT;
    }

    BiomeAtmosphere cached = CACHE.get(entry);
    if (cached != null) {
      return cached;
    }

    BiomeAtmosphere resolved = match(entry);
    CACHE.put(entry, resolved);

    return resolved;
  }

  private static BiomeAtmosphere match(RegistryEntry<Biome> entry) {
    for (BiomeAtmosphere atmosphere : values()) {
      if (atmosphere.tag != null && entry.isIn(atmosphere.tag)) {
        return atmosphere;
      }
    }

    return DEFAULT;
  }

  /** Drops the memo. Called when the player leaves a world, since registry entries are reloaded. */
  public static void clearCache() {
    CACHE.clear();
  }
}
