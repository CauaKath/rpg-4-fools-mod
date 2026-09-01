package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.world.CropDefinition;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
            Registries.ITEM_GROUP,
            new Identifier(RPG4Fools.MOD_ID, "crops"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.produceItem(ModCrops.TOMATO)))
                    .displayName(Text.translatable("itemGroup.rpg4fools.crops"))
                    .entries((context, entries) -> {
                      entries.add(ModItems.CROP_STICK);
                      entries.add(ModItems.CROP_WALL);

                      // The compost pipeline in the order it runs: what is gathered, then what is
                      // made from it.
                      ModCompostItems.all().forEach(entries::add);

                      for (CropDefinition definition : ModCrops.ALL) {
                        entries.add(ModItems.seedItem(definition));

                        // A bush's berry is its seed, so adding both would show it twice.
                        if (definition.kind() == CropDefinition.Kind.FARMLAND) {
                          entries.add(ModItems.produceItem(definition));
                        }
                      }
                    })
                    .build()
    );
  }
}
