package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.SeasonalFarmCrops;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

/**
 * Structure processors the mod adds.
 *
 * <p>Registered at init because the processor lists that name them are datapack files, and a name
 * the registry has never heard of fails to parse rather than falling back to nothing.
 */
public final class ModProcessors {
  public static final StructureProcessorType<SeasonalFarmCrops> SEASONAL_FARM_CROPS = () -> SeasonalFarmCrops.CODEC;

  private ModProcessors() {
  }

  public static void registerProcessors() {
    Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,
            new ResourceLocation(RPG4Fools.MOD_ID, "seasonal_farm_crops"), SEASONAL_FARM_CROPS);
  }
}
