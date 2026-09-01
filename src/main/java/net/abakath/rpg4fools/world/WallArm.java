package net.abakath.rpg4fools.world;

import net.minecraft.util.StringIdentifiable;

/**
 * Which of the three columns of a wall plant a cell sits in.
 *
 * <p>Half of a cell's address. The other half is its row, and the two together are how a cell an
 * arbitrary distance from the root still knows where its root is: a wall plant spreads to whichever
 * cell it likes, so a cell at the top of a column may have nothing underneath it, and walking down
 * the way a crop stick column does would find air.
 *
 * <p>Stored rather than derived, unlike {@link ColumnPart}. A stick derives its part because the
 * answer is only a sprite and a wrong one costs nothing; an arm is what the growth rules measure the
 * plant's box from, and there is no way to recover it by looking at the world - two plants growing
 * towards each other put their cells side by side, and nothing in those blocks says where one plant
 * ends and the next begins except this.
 *
 * <p>Offsets run along the plant's axis in the positive direction, so RIGHT is east on an X axis
 * plant and south on a Z axis one. Which way a player would call right depends on which side of the
 * wall they are standing on, so the names are only ever a label for the sign.
 */
public enum WallArm implements StringIdentifiable {
  LEFT("left", -1),
  CENTER("center", 0),
  RIGHT("right", 1);

  private final String name;
  private final int offset;

  WallArm(String name, int offset) {
    this.name = name;
    this.offset = offset;
  }

  /** How many blocks along the axis this arm sits from the root, signed. */
  public int offset() {
    return offset;
  }

  /** The arm at this signed offset, for anything working one out from a position. */
  public static WallArm at(int offset) {
    return switch (offset) {
      case -1 -> LEFT;
      case 1 -> RIGHT;
      default -> CENTER;
    };
  }

  @Override
  public String asString() {
    return name;
  }
}
