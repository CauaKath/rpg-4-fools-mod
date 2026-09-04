package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.server.events.crop.CropTagValidator;
import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Season;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Block tags the mod defines.
 *
 * <p>Season membership is data rather than a hardcoded map so a datapack can retag vanilla crops
 * and, more importantly, so crops from other mods can be given seasons without this mod knowing
 * they exist.
 */
public class ModBlockTags {
  /**
   * Everything the mod treats as a crop.
   *
   * <p>Kept separate from the per-season tags because "is a crop" and "grows in spring" are
   * different questions: a block with no season tag is still a crop, it just has no restriction
   * yet. This tag is also what CropTagValidator walks to report crops that were never given a
   * season.
   */
  public static final TagKey<Block> CROPS = of("crops");

  public static final TagKey<Block> GROWS_IN_SPRING = of("grows_in_spring");
  public static final TagKey<Block> GROWS_IN_SUMMER = of("grows_in_summer");
  public static final TagKey<Block> GROWS_IN_AUTUMN = of("grows_in_autumn");
  public static final TagKey<Block> GROWS_IN_WINTER = of("grows_in_winter");

  /** Exhaustive over Season, so adding a season to the enum will not compile until tagged here. */
  public static TagKey<Block> forSeason(Season season) {
    return switch (season) {
      case SPRING -> GROWS_IN_SPRING;
      case SUMMER -> GROWS_IN_SUMMER;
      case AUTUMN -> GROWS_IN_AUTUMN;
      case WINTER -> GROWS_IN_WINTER;
    };
  }

  private static TagKey<Block> of(String name) {
    return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, name));
  }
}
