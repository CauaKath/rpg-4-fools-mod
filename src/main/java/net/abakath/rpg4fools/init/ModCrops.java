package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.CropDefinition;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The plants this mod adds.
 *
 * <p>The single table everything else reads: block and item registration, the season tags, and the
 * lookup that maps a held item back to its crop. Keeping it in one place is what stops a crop from
 * being registered without a season, or tagged without being registered.
 *
 * <p>Seasons follow the growing calendar loosely, the way the vanilla assignments in
 * {@link net.abakath.rpg4fools.datagen.SeasonCropTagProvider} do. Nothing grows in winter.
 *
 * <p>Only blackberry is thorny. A bush inherits the vanilla sweet berry bush's habit of hurting
 * whatever walks through it, which suits a bramble and does not suit a strawberry patch.
 */
public final class ModCrops {
  public static final CropDefinition TOMATO = new CropDefinition(
          "tomato", CropDefinition.Kind.FARMLAND, seasons(Season.SUMMER), 3, 0.3f, false);

  public static final CropDefinition CUCUMBER = new CropDefinition(
          "cucumber", CropDefinition.Kind.FARMLAND, seasons(Season.SUMMER), 2, 0.2f, false);

  public static final CropDefinition LETTUCE = new CropDefinition(
          "lettuce", CropDefinition.Kind.FARMLAND, seasons(Season.SPRING, Season.AUTUMN), 2, 0.3f, false);

  public static final CropDefinition STRAWBERRY = new CropDefinition(
          "strawberry", CropDefinition.Kind.BUSH, seasons(Season.SPRING, Season.SUMMER), 2, 0.2f, false);

  public static final CropDefinition BLACKBERRY = new CropDefinition(
          "blackberry", CropDefinition.Kind.BUSH, seasons(Season.SUMMER, Season.AUTUMN), 2, 0.1f, true);

  public static final CropDefinition BLUEBERRY = new CropDefinition(
          "blueberry", CropDefinition.Kind.BUSH, seasons(Season.SUMMER), 2, 0.2f, false);

  /** Ordered, because the generated tag files follow this order and are committed. */
  public static final List<CropDefinition> ALL =
          List.of(TOMATO, CUCUMBER, LETTUCE, STRAWBERRY, BLACKBERRY, BLUEBERRY);

  private ModCrops() {
  }

  private static Set<Season> seasons(Season... grownIn) {
    return EnumSet.copyOf(List.of(grownIn));
  }
}
