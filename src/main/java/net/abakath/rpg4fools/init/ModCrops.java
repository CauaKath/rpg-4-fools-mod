package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.crop.CropDefinition;
import net.abakath.rpg4fools.world.crop.CropDefinition.Support;

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
 * <p>The number before the support is the age a pick leaves the plant at, and zero means picking is
 * not a thing this crop does: it is harvested by breaking it, as vanilla wheat is. Tomato and
 * cucumber fruit more than once, dropping back to age 4 of 7, so a ripe plant is worth keeping in
 * the ground for the rest of its season instead of tearing up.
 *
 * <p>The support says what carries the crop, and each one needs its own block, blockstate and a
 * model per age - so what it really names is which art the crop has. Tomato climbs sticks; cucumber
 * spreads over a wall. Nothing about either follows from the other, or from anything else here.
 *
 * <p>The last flag is winter. Nothing grows then, but kale stands through it: sown in autumn, frozen
 * at whatever age it reached, growing again in spring. It is the only crop on the roster that a
 * winter does not kill, and the flag exists rather than a special case because turnip and garlic
 * want the same thing in the batches after this one.
 *
 * <p>Only blackberry is thorny. A bush inherits the vanilla sweet berry bush's habit of hurting
 * whatever walks through it, which suits a bramble and does not suit a strawberry patch.
 */
public final class ModCrops {
  public static final CropDefinition TOMATO = new CropDefinition(
          "tomato", CropDefinition.Kind.FARMLAND, seasons(Season.SUMMER), 3, 0.3f, false, 4, Support.STICKED, false);

  public static final CropDefinition CUCUMBER = new CropDefinition(
          "cucumber", CropDefinition.Kind.FARMLAND, seasons(Season.SUMMER), 2, 0.2f, false, 4, Support.WALLED, false);

  public static final CropDefinition LETTUCE = new CropDefinition(
          "lettuce", CropDefinition.Kind.FARMLAND, seasons(Season.SPRING, Season.AUTUMN), 2, 0.3f, false, 0, Support.NONE, false);

  public static final CropDefinition CABBAGE = new CropDefinition(
          "cabbage", CropDefinition.Kind.FARMLAND, seasons(Season.SPRING, Season.AUTUMN), 4, 0.5f, false, 0, Support.NONE, false);

  public static final CropDefinition KALE = new CropDefinition(
          "kale", CropDefinition.Kind.FARMLAND, seasons(Season.AUTUMN, Season.SPRING), 2, 0.4f, false, 4, Support.NONE, true);

  public static final CropDefinition STRAWBERRY = new CropDefinition(
          "strawberry", CropDefinition.Kind.BUSH, seasons(Season.SPRING, Season.SUMMER), 2, 0.2f, false, 0, Support.NONE, false);

  public static final CropDefinition BLACKBERRY = new CropDefinition(
          "blackberry", CropDefinition.Kind.BUSH, seasons(Season.SUMMER, Season.AUTUMN), 2, 0.1f, true, 0, Support.NONE, false);

  public static final CropDefinition BLUEBERRY = new CropDefinition(
          "blueberry", CropDefinition.Kind.BUSH, seasons(Season.SUMMER), 2, 0.2f, false, 0, Support.NONE, false);

  /** Ordered, because the generated tag files follow this order and are committed. */
  public static final List<CropDefinition> ALL =
          List.of(TOMATO, CUCUMBER, LETTUCE, CABBAGE, KALE, STRAWBERRY, BLACKBERRY, BLUEBERRY);

  private ModCrops() {
  }

  private static Set<Season> seasons(Season... grownIn) {
    return EnumSet.copyOf(List.of(grownIn));
  }
}
