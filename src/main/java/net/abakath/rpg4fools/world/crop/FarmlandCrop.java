package net.abakath.rpg4fools.world.crop;

import net.abakath.rpg4fools.enums.Season;

import java.util.Set;

/**
 * A plant sown on farmland: eight ages, a seed of its own, and a season that kills it.
 *
 * @param regrowAge the age a pick leaves the plant at, or zero for a crop that is harvested by
 *     breaking it, as vanilla wheat is. Not a {@link Support} of its own and not a kind of crop: a
 *     tomato that fruits twice is still sown on farmland, still has a seed and produce, still dies
 *     when its season ends. Regrowth is one more thing a farmland crop can do, so it is one more
 *     field.
 * @param support what holds this crop up, and so which extra block it gets. One field rather than a
 *     flag per shape: the shapes are mutually exclusive, and as flags most of the combinations would
 *     have been nonsense the compiler was happy to accept. Each shape needs its own block,
 *     blockstate and a model per age, so what this field really says is which art the crop has -
 *     which is why nothing about it follows from anything else on the crop. A vine that sheets
 *     across a trellis, a plant that climbs a post and a stalk that holds itself up are three
 *     different pictures.
 * @param survivesWinter whether this crop stands through winter instead of dying, frozen at whatever
 *     age it reached. A bush cannot be asked this at all: it has its own way of sitting a winter out,
 *     swapping for a dormant block, and a plant that claimed both would be asking two rules for two
 *     different answers. That is one of the pairings splitting the record made unrepresentable.
 */
public record FarmlandCrop(
        String id,
        Set<Season> seasons,
        int nutrition,
        float saturation,
        int regrowAge,
        Support support,
        boolean survivesWinter
) implements CropDefinition {
  /** Whether picking this crop leaves the plant standing. */
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

  /**
   * What carries a crop, and so which second block it is registered with.
   *
   * <p>Read as an either-or rather than a set of flags. A crop drawn for one of these has no art for
   * any of the others, and the two supports also differ in where a plant may spread, so a crop
   * offered a support it was not drawn for is a missing texture at best.
   */
  public enum Support {
    /** Stands on its own, one block tall. What most farmland crops are. */
    NONE,
    /** Climbs a trellis of crop sticks, up to three sections tall. */
    STICKED,
    /** Spreads over the panels of a crop wall. */
    WALLED,
    /** Holds itself up, two sections tall, with no support to build. */
    TALL
  }

  @Override
  public String blockName() {
    return id + "_crop";
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

  @Override
  public String produceName() {
    return id;
  }
}
