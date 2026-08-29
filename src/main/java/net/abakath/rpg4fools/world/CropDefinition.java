package net.abakath.rpg4fools.world;

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
 */
public record CropDefinition(
        String id,
        Kind kind,
        Set<Season> seasons,
        int nutrition,
        float saturation,
        boolean thorny,
        int regrowAge
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

  public enum Kind {
    /** Sown on farmland, grows through eight ages, dies when its season ends. */
    FARMLAND,
    /** Planted where a bush can stand, four ages, goes dormant instead of dying. */
    BUSH
  }

  public String blockName() {
    return kind == Kind.FARMLAND ? id + "_crop" : id + "_bush";
  }

  public String dormantBlockName() {
    return "dormant_" + id + "_bush";
  }

  public String seedName() {
    return id + "_seeds";
  }

  /** For a bush this is also the seed: a berry plants the bush it came from, as sweet berries do. */
  public String produceName() {
    return kind == Kind.FARMLAND ? id : id + "_berries";
  }
}
