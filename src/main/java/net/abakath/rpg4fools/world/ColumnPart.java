package net.abakath.rpg4fools.world;

import net.minecraft.util.StringIdentifiable;

/**
 * Where a section sits in the plant it belongs to.
 *
 * <p>Only the sprite depends on this. A tomato climbing a trellis is one plant, so it needs roots at
 * the foot and a tip at the crown, and the block has no other way to know which end of the plant it
 * is. Nothing about growth or harvest reads it.
 *
 * <p>Never stored by anything that sets a section down. {@link StickedCropBlock} works it out from
 * the blocks above and below on every neighbour update, so it cannot drift out of step with a column
 * that was broken, extended, or reduced to bare sticks by a season.
 */
public enum ColumnPart implements StringIdentifiable {
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
  public String asString() {
    return name;
  }
}
