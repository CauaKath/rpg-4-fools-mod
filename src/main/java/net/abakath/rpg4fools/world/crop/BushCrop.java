package net.abakath.rpg4fools.world.crop;

import net.abakath.rpg4fools.enums.Season;

import java.util.Set;

/**
 * A bush: four ages, a berry that is also its seed, and a winter it sits out rather than dies in.
 *
 * <p>No support and no regrow age. A bush stands on its own, and picking it is how it is harvested
 * at all rather than a second way of harvesting it - both of which a farmland crop has to be asked
 * about and a bush does not.
 *
 * @param thorny whether walking through this bush hurts, the way the vanilla sweet berry bush does.
 *     It suits a bramble and does not suit a strawberry patch, and it is the one thing a bush is
 *     asked that a farmland crop never is.
 */
public record BushCrop(
        String id,
        Set<Season> seasons,
        int nutrition,
        float saturation,
        boolean thorny
) implements CropDefinition {
  @Override
  public String blockName() {
    return id + "_bush";
  }

  /** The block this bush swaps itself for over winter. */
  public String dormantBlockName() {
    return "dormant_" + id + "_bush";
  }

  /**
   * The berry, which is also the seed: a berry plants the bush it came from, as sweet berries do.
   *
   * <p>Which is why there is no seed name here. One item does both jobs, and a second name would be
   * a second item nothing registers.
   */
  @Override
  public String produceName() {
    return id + "_berries";
  }
}
