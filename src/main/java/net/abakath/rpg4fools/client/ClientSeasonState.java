package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.SubSeason;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Client-side view of the current season.
 *
 * <p>The season only ever affects rendering, so the client keeps its own copy instead of
 * reaching for the server-side {@link net.abakath.rpg4fools.server.SeasonData}. The value is
 * derived from the date carried by
 * {@link net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket}.
 *
 * <p>The day within the month is tracked as well, so the tint can be interpolated towards the next
 * sub season instead of snapping on the first of the month.
 */
@Environment(EnvType.CLIENT)
public final class ClientSeasonState {
  /** Must match DayChangingHandler.MONTH_DURATION. */
  private static final int MONTH_DURATION = 28;

  private static volatile SubSeason subSeason = SubSeason.EARLY_SPRING;

  /** Progress through the current sub season, 0 on the first day and approaching 1 on the last. */
  private static volatile float progress = 0.0f;

  /**
   * The date as last sent. Held here rather than on the player, because the client player entity is
   * rebuilt on respawn and on a dimension change, which would drop it. That went unnoticed while
   * the date was resent every tick.
   */
  private static volatile int year = 1;
  private static volatile int month = 0;
  private static volatile int day = 1;

  private static int lastMonth = -1;
  private static int lastDay = -1;

  private ClientSeasonState() {
  }

  public static SubSeason getSubSeason() {
    return subSeason;
  }

  public static float getProgress() {
    return progress;
  }

  public static int getYear() {
    return year;
  }

  public static int getMonthOrdinal() {
    return month;
  }

  public static int getDay() {
    return day;
  }

  /**
   * Updates the cached season from the current date. When the rendered tint changes the loaded
   * chunks are queued for a rebuild so every one of them picks up the new colour.
   *
   * <p>The tint moves once per in game day, so this queues one rebuild per day rather than one per
   * sub season. That is a deliberate trade of a rebuild for a smooth transition.
   *
   * <p>Must be called from the client thread.
   */
  public static void update(int year, int monthOrdinal, int day) {
    if (monthOrdinal < 0 || monthOrdinal >= Months.values().length) {
      return;
    }

    // Assigned before the guard below, so a repeat for a joining player still refreshes the date
    // even though there is no tint work left to do for it.
    ClientSeasonState.year = year;
    ClientSeasonState.month = monthOrdinal;
    ClientSeasonState.day = day;

    if (monthOrdinal == lastMonth && day == lastDay) {
      return;
    }

    lastMonth = monthOrdinal;
    lastDay = day;

    subSeason = Months.values()[monthOrdinal].getSubSeason();

    // Keeps the client half of the precipitation mixin in step, so rain renders as snow at the same
    // moment the server starts laying it down.
    CurrentSeason.set(subSeason);
    progress = ColorMath.clamp01((float) (day - 1) / MONTH_DURATION);

    refreshTintedChunks(Minecraft.getInstance());
  }

  /**
   * Drops the cached biome colours and marks the loaded sections for a rebuild, so the new tint
   * reaches the geometry it is baked into.
   *
   * <p>Deliberately not {@code WorldRenderer#reload}. That clears the built chunk storage and
   * allocates a fresh one, which drops every uploaded vertex buffer, so the world draws empty until
   * the rebuild catches up. Once a day that reads as the world visibly re-rendering itself.
   * Scheduling a rebuild queues the same work while the existing geometry keeps drawing, so the
   * recolour arrives without the blank frames.
   */
  private static void refreshTintedChunks(Minecraft client) {
    ClientLevel world = client.level;
    if (world == null || client.levelRenderer == null || client.player == null) {
      return;
    }

    // The colour providers read through BiomeColors, which memoises per position. Without this the
    // rebuilt chunks would come back carrying the previous day's colours.
    world.clearTintCaches();

    // Every section the storage holds, marked in one range call.
    //
    // setSectionRangeDirty takes section coordinates and walks them inclusively, marking each one.
    // Not setBlocksDirty, which reads as the obvious fit and is not: it takes block coordinates and
    // shifts them down to sections itself, so a whole view distance handed to it would loop over
    // every block in the world height, hundreds of millions of iterations for the hundred thousand
    // sections underneath them.
    //
    // The horizontal span is deliberately 2 * viewDistance + 1, the width of the section storage
    // itself. It indexes by coordinate modulo that width, so a run of exactly that many consecutive
    // coordinates lands on every slot once, wherever the storage happens to be centred, and the
    // camera sitting off the player cannot leave a column out. The view distance is read back from
    // the options because the level renderer reloads itself whenever the two disagree.
    //
    // getMaxSectionY is the last section rather than one past it, which is what the inclusive
    // bounds of setSectionRangeDirty want.
    int radius = client.options.getEffectiveRenderDistance();
    ChunkPos center = client.player.chunkPosition();

    world.setSectionRangeDirty(
            center.x() - radius, world.getMinSectionY(), center.z() - radius,
            center.x() + radius, world.getMaxSectionY(), center.z() + radius);
  }
}
