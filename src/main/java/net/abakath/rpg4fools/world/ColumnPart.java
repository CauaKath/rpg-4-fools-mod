package net.abakath.rpg4fools.world;

import net.minecraft.util.StringRepresentable;

/**
 * Where a section sits in the plant it belongs to.
 *
 * <p>Only the sprite depends on this. A tomato climbing a trellis is one plant, so it needs roots at
 * the foot and a tip at the crown, and the block has no other way to know which end of the plant it
 * is. Nothing about growth or harvest reads it.
 *
 * <p>Never stored by anything that sets a block down. Both blocks in a column work it out from what
 * is above and below on every neighbour update, so it cannot drift out of step with a column that was
 * broken, extended, or reduced to bare sticks by a season.
 *
 * <p>They disagree about what counts as a neighbour, deliberately. A plant asks about sections of
 * itself, because its foot and crown are the ends of the plant; a stick asks about any column block,
 * because a stick above a plant is the top of that column whether or not it is part of the plant.
 */
public enum ColumnPart implements StringRepresentable {
  /** The whole plant, one section tall. What a tomato on a single stick looks like. */
  SINGLE("single"),
  /** Rooted, with more plant above. */
  BOTTOM("bottom"),
  /** Plant above and below. Only reachable on a plant three sections tall. */
  MIDDLE("middle"),
  /** The crown, with more plant below. */
  TOP("top");

  private final String name;

  ColumnPart(String name) {
    this.name = name;
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
