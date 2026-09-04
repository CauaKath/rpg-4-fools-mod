package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Entity tags the mod defines.
 *
 * <p>One so far. A tag rather than a list in code so a datapack can add an animal from another mod
 * to the pen without this mod knowing it exists, which is the same reason the season tags are data.
 */
public class ModEntityTags {
  /** Animals that leave manure behind. Grazers, which is what makes manure a farm by-product. */
  public static final TagKey<EntityType<?>> MANURE_PRODUCERS = of("manure_producers");

  private static TagKey<EntityType<?>> of(String name) {
    return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, name));
  }
}
