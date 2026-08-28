package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.enums.SubSeason;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * Client-side view of the current season.
 *
 * <p>The season only ever affects rendering, so the client keeps its own copy instead of
 * reaching for the server-side {@link net.abakath.rpg4fools.server.SeasonData}. The value is
 * derived from the month carried by
 * {@link net.abakath.rpg4fools.network.packets.s2c.SeasonUpdatePacket}.
 */
@Environment(EnvType.CLIENT)
public final class ClientSeasonState {
  private static volatile SubSeason subSeason = SubSeason.EARLY_SPRING;

  private ClientSeasonState() {
  }

  public static SubSeason getSubSeason() {
    return subSeason;
  }

  /**
   * Updates the cached season from a month ordinal. When the sub season actually changes the
   * world renderer is reloaded so every chunk picks up the new tint.
   *
   * <p>Must be called from the client thread.
   */
  public static void update(int monthOrdinal) {
    if (monthOrdinal < 0 || monthOrdinal >= Months.values().length) {
      return;
    }

    SubSeason newSubSeason = Months.values()[monthOrdinal].getSubSeason();

    if (newSubSeason == subSeason) {
      return;
    }

    subSeason = newSubSeason;

    MinecraftClient client = MinecraftClient.getInstance();
    if (client.worldRenderer != null) {
      client.worldRenderer.reload();
    }
  }
}
