package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;

import java.util.function.Predicate;

/**
 * Where the mod's bushes grow on their own.
 *
 * <p>The patches themselves are data, copied from the vanilla berry patch: bushes on grass, placed
 * already fruiting. Strawberry and blackberry are rarer and smaller than the vanilla original,
 * because plains and forest are far bigger biomes than taiga and a vanilla rate there reads as a
 * strawberry field rather than a lucky find.
 *
 * <p>What cannot be data is which biomes they belong to, since a datapack can only add a feature to
 * a vanilla biome by replacing the whole biome file, and that would overwrite everything else the
 * biome does. Fabric's biome API adds them instead.
 *
 * <p>One bush to a climate, so the biome tells you what you will find.
 */
public final class ModWorldGen {
  private static final Predicate<BiomeSelectionContext> MEADOWS =
          BiomeSelectors.includeByKey(BiomeKeys.MEADOW, BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS);

  private static final Predicate<BiomeSelectionContext> FORESTS =
          BiomeSelectors.includeByKey(BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST, BiomeKeys.DARK_FOREST);

  private static final Predicate<BiomeSelectionContext> TAIGAS =
          BiomeSelectors.includeByKey(BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA);

  public static void registerFeatures() {
    grow(patchOf(ModCrops.STRAWBERRY), MEADOWS);
    grow(patchOf(ModCrops.BLACKBERRY), FORESTS);

    shareTheTaiga();
  }

  private ModWorldGen() {
  }

  /**
   * Splits the taiga's berries between blueberry and sweet berry without adding any.
   *
   * <p>Adding blueberry beside the vanilla patch would have doubled how many bushes a taiga has.
   * The vanilla patch is dropped instead and two halves put back, each at half the rate, so the
   * taiga keeps the one patch every 32 chunks it always had and a patch is either all blueberry or
   * all sweet berry.
   *
   * <p>The sweet berry half is this mod's own copy of the vanilla patch rather than the vanilla
   * feature itself, because a rarity filter lives on the placed feature and the vanilla one is not
   * ours to retune. Snowy taiga is left alone: its berries come from patch_berry_rare, blueberry
   * does not grow there, and there is nothing to split.
   */
  private static void shareTheTaiga() {
    BiomeModifications.create(new Identifier(RPG4Fools.MOD_ID, "taiga_berries"))
            .add(ModificationPhase.REMOVALS, TAIGAS, context ->
                    context.getGenerationSettings().removeFeature(
                            GenerationStep.Feature.VEGETAL_DECORATION,
                            VegetationPlacedFeatures.PATCH_BERRY_COMMON
                    ));

    grow(patchOf(ModCrops.BLUEBERRY), TAIGAS);
    grow(patch("patch_sweet_berry_bush"), TAIGAS);
  }

  private static void grow(RegistryKey<PlacedFeature> patch, Predicate<BiomeSelectionContext> biomes) {
    BiomeModifications.addFeature(biomes, GenerationStep.Feature.VEGETAL_DECORATION, patch);
  }

  /**
   * Feature ids are derived from the block name for the same reason every other name in
   * {@link CropDefinition} is: a patch pointing at a bush that spells itself differently would
   * still load, and would simply never generate.
   */
  private static RegistryKey<PlacedFeature> patchOf(CropDefinition definition) {
    return patch("patch_" + definition.blockName());
  }

  private static RegistryKey<PlacedFeature> patch(String name) {
    return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(RPG4Fools.MOD_ID, name));
  }
}
