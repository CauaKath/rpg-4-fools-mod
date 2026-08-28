package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.SubSeason;
import net.abakath.rpg4fools.world.SeasonSnow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

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
   * Updates the cached season from the current date. When the rendered tint changes the world
   * renderer is reloaded so every chunk picks up the new colour.
   *
   * <p>The tint moves once per in game day, so this triggers one renderer reload per day rather
   * than one per sub season. That is a deliberate trade of a rebuild for a smooth transition.
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
    SeasonSnow.setSubSeason(subSeason);
    progress = ColorMath.clamp01((float) (day - 1) / MONTH_DURATION);

    MinecraftClient client = MinecraftClient.getInstance();
    if (client.worldRenderer != null) {
      client.worldRenderer.reload();
    }
  }
}
