package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Where the mod's bushes grow on their own.
 *
 * <p>The patches themselves are data, copied from the vanilla berry patch: bushes on grass, placed
 * already fruiting. Blueberry keeps the vanilla numbers, since it grows in the taiga next to the
 * sweet berries it is copying. Strawberry and blackberry are quartered and thinned, because plains
 * and forest are far bigger biomes than taiga and a vanilla rate there reads as a strawberry field
 * rather than a lucky find.
 *
 * <p>What cannot be data is which biomes they belong to, since a datapack can only add a feature to
 * a vanilla biome by replacing the whole biome file, and that would overwrite everything else the
 * biome does. Fabric's biome API adds them instead.
 *
 * <p>One bush to a climate, so the biome tells you what you will find. Only blueberry shares ground
 * with vanilla sweet berries, which is the taiga's to begin with.
 *
 * <p>Seasons are not a worry here the way they are for village crops. A patch that generates out of
 * season goes dormant on its first random tick rather than dying, so a winter world still has its
 * bushes, just bare ones.
 */
public final class ModWorldGen {
  public static void registerFeatures() {
    grow(ModCrops.STRAWBERRY, BiomeKeys.MEADOW, BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS);
    grow(ModCrops.BLACKBERRY, BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.DARK_FOREST);
    grow(ModCrops.BLUEBERRY, BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA);
  }

  private ModWorldGen() {
  }

  /**
   * Feature ids are derived from the block name for the same reason every other name in
   * {@link CropDefinition} is: a patch pointing at a bush that spells itself differently would
   * still load, and would simply never generate.
   */
  @SafeVarargs
  private static void grow(CropDefinition definition, RegistryKey<Biome>... biomes) {
    RegistryKey<PlacedFeature> patch = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
            new Identifier(RPG4Fools.MOD_ID, "patch_" + definition.blockName()));

    BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(biomes),
            GenerationStep.Feature.VEGETAL_DECORATION,
            patch
    );
  }
}
