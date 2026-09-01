package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.SubSeason;
import net.abakath.rpg4fools.world.CurrentSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;

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
  /** Days in a month, matching DayChangingHandler.MONTH_DURATION. */
  private static final int MONTH_DURATION = 28;

  private static volatile SubSeason subSeason = SubSeason.EARLY_SPRING;

  /** Progress through the current sub season, 0 on the first day and approaching 1 on the last. */
  private static volatile float progress = 0.0f;

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

  /**
   * Updates the cached season from the current date. When the rendered tint changes the loaded
   * chunks are queued for a rebuild so every one of them picks up the new colour.
   *
   * <p>The tint moves once per in game day, so this queues one rebuild per day rather than one per
   * sub season. That is a deliberate trade of a rebuild for a smooth transition.
   *
   * <p>Must be called from the client thread.
   */
  public static void update(int monthOrdinal, int day) {
    if (monthOrdinal < 0 || monthOrdinal >= Months.values().length) {
      return;
    }

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

    refreshTintedChunks(MinecraftClient.getInstance());
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
  private static void refreshTintedChunks(MinecraftClient client) {
    ClientWorld world = client.world;
    if (world == null || client.worldRenderer == null || client.player == null) {
      return;
    }

    // The colour providers read through BiomeColors, which memoises per position. Without this the
    // rebuilt chunks would come back carrying the previous day's colours.
    world.reloadColor();

    // BuiltChunkStorage indexes its array by chunk coordinate modulo its own size, and that size is
    // 2 * viewDistance + 1 on both horizontal axes and the section count vertically. A run of
    // exactly that many consecutive coordinates therefore covers every slot once, wherever the
    // storage happens to be centred, so the camera sitting off the player is not a problem. The
    // view distance is read back from the options because WorldRenderer reloads itself whenever the
    // two disagree, which keeps the storage the size assumed here.
    int radius = client.options.getClampedViewDistance();
    ChunkPos center = client.player.getChunkPos();

    client.worldRenderer.scheduleBlockRenders(
            center.x - radius, world.getBottomSectionCoord(), center.z - radius,
            center.x + radius, world.getTopSectionCoord() - 1, center.z + radius
    );
  }
}
