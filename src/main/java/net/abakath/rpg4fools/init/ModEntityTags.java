package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.RPG4Fools;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

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
    return TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(RPG4Fools.MOD_ID, name));
  }
}
