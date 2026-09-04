package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The mod's creative tab.
 *
 * <p>The only way to get these items right now: nothing drops them, no villager sells them and no
 * chest holds them. Survival acquisition is a later change, and until it lands the tab is what makes
 * the crops reachable at all.
 */
public final class ModItemGroups {
  private ModItemGroups() {
  }

  public static void registerItemGroups() {
    Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            new ResourceLocation(RPG4Fools.MOD_ID, "crops"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.produceItem(ModCrops.TOMATO)))
                    .title(Component.translatable("itemGroup.rpg4fools.crops"))
                    .displayItems((context, entries) -> {
                      entries.accept(ModItems.CROP_STICK);
                      entries.accept(ModItems.CROP_WALL);

                      // The compost pipeline in the order it runs: what is gathered, then what is
                      // made from it.
                      ModCompostItems.all().forEach(entries::accept);

                      for (CropDefinition definition : ModCrops.ALL) {
                        entries.accept(ModItems.seedItem(definition));

                        // A bush's berry is its seed, so adding both would show it twice.
                        if (definition.kind() == CropDefinition.Kind.FARMLAND) {
                          entries.accept(ModItems.produceItem(definition));
                        }
                      }
                    })
                    .build()
    );
  }
}
