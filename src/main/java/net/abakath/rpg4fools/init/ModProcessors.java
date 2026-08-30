package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.SeasonalFarmCrops;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;

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
    Registry.register(Registries.STRUCTURE_PROCESSOR,
            new Identifier(RPG4Fools.MOD_ID, "seasonal_farm_crops"), SEASONAL_FARM_CROPS);
  }
}
