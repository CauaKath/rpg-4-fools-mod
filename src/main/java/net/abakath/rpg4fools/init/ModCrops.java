package net.abakath.rpg4fools.init;

import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.world.crop.BushCrop;
import net.abakath.rpg4fools.world.crop.CropDefinition;
import net.abakath.rpg4fools.world.crop.FarmlandCrop;
import net.abakath.rpg4fools.world.crop.FarmlandCrop.Support;

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
 * <p>The two exceptions a plant can be are named as lists rather than carried as a flag on every
 * entry. Winter and thorns are things almost nothing on the roster is, and a column of falses is a
 * column nobody reads: it says the same thing eight times and hides the one line where it changes.
 * Named here, the exception is the entry, and adding a crop to it is a word rather than a rewrite of
 * its line. {@link #check()} is what keeps the lists honest.
 */
public final class ModCrops {
  /**
   * The crops a winter leaves standing, frozen at whatever age they reached and growing again in
   * spring. Kale is sown in autumn for exactly this; turnip and garlic want the same thing in the
   * batches after it.
   *
   * <p>Farmland only. A bush has its own way of sitting a winter out, so there is no
   * {@code survivesWinter} to give one - see {@link net.abakath.rpg4fools.world.crop.BushCrop}.
   */
  private static final Set<String> SURVIVES_WINTER = Set.of("kale");

  /**
   * The bushes that hurt to walk through, the way the vanilla sweet berry bush does. It suits a
   * bramble and does not suit a strawberry patch.
   */
  private static final Set<String> THORNY = Set.of("blackberry");

  public static final FarmlandCrop TOMATO =
          farmland("tomato", seasons(Season.SUMMER), 3, 0.3f, 4, Support.STICKED);

  public static final FarmlandCrop CUCUMBER =
          farmland("cucumber", seasons(Season.SUMMER), 2, 0.2f, 4, Support.WALLED);

  public static final FarmlandCrop LETTUCE =
          farmland("lettuce", seasons(Season.SPRING, Season.AUTUMN), 2, 0.3f, 0, Support.NONE);

  public static final FarmlandCrop CABBAGE =
          farmland("cabbage", seasons(Season.SPRING, Season.AUTUMN), 4, 0.5f, 0, Support.NONE);

  public static final FarmlandCrop KALE =
          farmland("kale", seasons(Season.AUTUMN, Season.SPRING), 2, 0.4f, 4, Support.NONE);

  public static final FarmlandCrop SPINACH =
          farmland("spinach", seasons(Season.SPRING, Season.AUTUMN), 1, 0.3f, 5, Support.NONE);

  public static final BushCrop STRAWBERRY =
          bush("strawberry", seasons(Season.SPRING, Season.SUMMER), 2, 0.2f);

  public static final BushCrop BLACKBERRY =
          bush("blackberry", seasons(Season.SUMMER, Season.AUTUMN), 2, 0.1f);

  public static final BushCrop BLUEBERRY =
          bush("blueberry", seasons(Season.SUMMER), 2, 0.2f);

  /** Ordered, because the generated tag files follow this order and are committed. */
  public static final List<CropDefinition> ALL =
          List.of(TOMATO, CUCUMBER, LETTUCE, CABBAGE, KALE, SPINACH, STRAWBERRY, BLACKBERRY, BLUEBERRY);

  static {
    check();
  }

  private ModCrops() {
  }

  private static FarmlandCrop farmland(String id, Set<Season> seasons, int nutrition, float saturation,
                                       int regrowAge, Support support) {
    return new FarmlandCrop(id, seasons, nutrition, saturation, regrowAge, support,
            SURVIVES_WINTER.contains(id));
  }

  private static BushCrop bush(String id, Set<Season> seasons, int nutrition, float saturation) {
    return new BushCrop(id, seasons, nutrition, saturation, THORNY.contains(id));
  }

  private static Set<Season> seasons(Season... grownIn) {
    return EnumSet.copyOf(List.of(grownIn));
  }

  /**
   * Fails the mod at load if either list names a plant the roster does not have.
   *
   * <p>The lists are matched to crops by name, which is the one thing about them that can go wrong
   * quietly: a crop renamed or a name mistyped leaves a plant that simply never survives a winter,
   * and a season is a long way to go to find that out. Nothing checks that a plant is on a list -
   * not being on one is the ordinary case - only that everything on a list exists and is the shape
   * the list is about.
   */
  private static void check() {
    for (String id : SURVIVES_WINTER) {
      if (ALL.stream().noneMatch(crop -> crop instanceof FarmlandCrop && crop.id().equals(id))) {
        throw new IllegalStateException(id + " is on the winter list but is not a farmland crop");
      }
    }

    for (String id : THORNY) {
      if (ALL.stream().noneMatch(crop -> crop instanceof BushCrop && crop.id().equals(id))) {
        throw new IllegalStateException(id + " is on the thorny list but is not a bush");
      }
    }
  }
}
