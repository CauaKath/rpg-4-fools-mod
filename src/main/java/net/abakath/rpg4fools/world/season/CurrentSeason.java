package net.abakath.rpg4fools.world.season;

import net.abakath.rpg4fools.client.season.ClientSeasonState;
import net.abakath.rpg4fools.enums.Season;
import net.abakath.rpg4fools.enums.SubSeason;

/**
 * The season currently in effect.
 *
 * <p>Held statically rather than passed around because both sides need it and neither has a handle
 * on the other's season state: the server owns SeasonData, the client owns ClientSeasonState. Both
 * write here. In single player they share a JVM and write the same value, which is harmless.
 *
 * <p>Kept out of persistent state on purpose. Callers include block random ticks and player
 * interactions, where the answer has to cost a single volatile read.
 */
public final class CurrentSeason {
  private static volatile SubSeason subSeason = SubSeason.EARLY_SPRING;

  private CurrentSeason() {
  }

  public static void set(SubSeason current) {
    if (current != null) {
      subSeason = current;
    }
  }

  public static SubSeason get() {
    return subSeason;
  }

  /** The parent season, for anything comparing against per-season crop tags. */
  public static Season season() {
    return subSeason.getSeason();
  }
}
