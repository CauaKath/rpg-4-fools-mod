package net.abakath.rpg4fools.world.crop;

import net.abakath.rpg4fools.enums.Season;

import java.util.Set;

/**
 * One plant the mod adds, as data.
 *
 * <p>Blocks, items, season tags and the item lookup are all derived from this, so a crop cannot be
 * registered without a season or given an item its block does not know about. Adding a plant is one
 * entry in {@link net.abakath.rpg4fools.init.ModCrops}.
 *
 * <p>Names are derived rather than stored. A crop that spelled its block one way and its seed
 * another would still compile, and the mistake would only show up as a missing texture.
 *
 * @param support what holds this crop up, and so which extra block it gets. One field rather than a
 *     flag per shape: the shapes are mutually exclusive, and as flags most of the combinations would
 *     have been nonsense the compiler was happy to accept. Each shape needs its own block,
 *     blockstate and a model per age, so what this field really says is which art the crop has -
 *     which is why nothing about it follows from anything else on the crop. A vine that sheets
 *     across a trellis, a plant that climbs a post and a stalk that holds itself up are three
 *     different pictures.
 */
public record CropDefinition(
        String id,
        Kind kind,
        Set<Season> seasons,
        int nutrition,
        float saturation,
        boolean thorny,
        int regrowAge,
        Support support
) {
  /**
   * Whether picking this crop leaves the plant standing.
   *
   * <p>Not a {@link Kind} of its own. A tomato that fruits twice is still sown on farmland, still
   * has a seed and produce, still dies when its season ends; every place that asks about FARMLAND
   * means it too. Regrowth is one more thing a farmland crop can do, so it is one more field.
   */
  public boolean regrows() {
    return regrowAge > 0;
  }

  /** Whether crop sticks can carry this crop. */
  public boolean sticked() {
    return support == Support.STICKED;
  }

  /** Whether a crop wall can carry this crop. */
  public boolean walled() {
    return support == Support.WALLED;
  }

  public enum Kind {
    /** Sown on farmland, grows through eight ages, dies when its season ends. */
    FARMLAND,
    /** Planted where a bush can stand, four ages, goes dormant instead of dying. */
    BUSH
  }

  /**
   * What carries a crop, and so which second block it is registered with.
   *
   * <p>Read as an either-or rather than a set of flags. A crop drawn for one of these has no art for
   * any of the others, and the two supports also differ in where a plant may spread, so a crop
   * offered a support it was not drawn for is a missing texture at best.
   */
  public enum Support {
    /** Stands on its own, one block tall. What most farmland crops are, and every bush. */
    NONE,
    /** Climbs a trellis of crop sticks, up to three sections tall. */
    STICKED,
    /** Spreads over the panels of a crop wall. */
    WALLED,
    /** Holds itself up, two sections tall, with no support to build. */
    TALL
  }

  public String blockName() {
    return kind == Kind.FARMLAND ? id + "_crop" : id + "_bush";
  }

  public String dormantBlockName() {
    return "dormant_" + id + "_bush";
  }

  /** The sticked form of this crop's block. Only meaningful when {@link #sticked()}. */
  public String stickedBlockName() {
    return id + "_crop_stick";
  }

  /** The walled form of this crop's block. Only meaningful when {@link #walled()}. */
  public String walledBlockName() {
    return id + "_crop_wall";
  }

  public String seedName() {
    return id + "_seeds";
  }

  /** For a bush this is also the seed: a berry plants the bush it came from, as sweet berries do. */
  public String produceName() {
    return kind == Kind.FARMLAND ? id : id + "_berries";
  }
}
