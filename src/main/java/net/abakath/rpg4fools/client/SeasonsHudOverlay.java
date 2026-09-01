package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Holiday;
import net.abakath.rpg4fools.enums.Months;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The season badge and the day change message.
 *
 * <p>A HUD element rather than a render callback: the HUD is extracted into a render state ahead of
 * drawing now, so this contributes to that extraction and the GPU work happens later, off this
 * call. What is drawn and where is unchanged.
 */
public class SeasonsHudOverlay implements HudElement {
  private static final int SEASON_OVERLAY_SCALE = 16;

  /** Length of a Minecraft day in ticks. */
  private static final int DAY_DURATION = 24000;

  /** How long the day change message stays on screen, in ticks. 160 ticks is 8 seconds. */
  private static final int MESSAGE_DURATION = 160;

  /** Ticks spent fading in at the start and fading out at the end. */
  private static final int MESSAGE_FADE_DURATION = 20;

  private static final int MIN_TEXT_ALPHA = 0x20;
  private static final int MAX_TEXT_ALPHA = 0xFF;

  @Override
  public void extractRenderState(GuiGraphicsExtractor drawContext, DeltaTracker tickDelta) {
    Minecraft client = Minecraft.getInstance();

    assert client != null;
    assert client.player != null;
    if (client.player.hasInfiniteMaterials() || client.player.isSpectator()) {
      return;
    }

    assert client.level != null;
    if (!client.level.dimension().equals(Level.OVERWORLD)) {
      return;
    }

    int width = drawContext.guiWidth();
    int height = drawContext.guiHeight();

    int x = getHalf(width) - getHalf(SEASON_OVERLAY_SCALE);
    int y = height - (SEASON_OVERLAY_SCALE * 3);

    int year = ClientSeasonState.getYear();
    int month = ClientSeasonState.getMonthOrdinal();
    int day = ClientSeasonState.getDay();

    // Read from the world rather than from the date packet. The message fades over its first 160
    // ticks, which needs a tick accurate value, and the client already advances world time itself.
    long dayTime = client.level.getOverworldClockTime();

    Months currentMonth = Months.values()[month];
    Holiday holiday = Holiday.getHoliday(day, (month + 1));

    if (holiday != null) {
      blitBadge(drawContext, holiday.getHolidayTexture(), x, y);
    } else {
      blitBadge(drawContext, currentMonth.getSubSeason().getSeason().getSeasonTexture(), x, y);
    }

    boolean newDay = isNewDay(dayTime);

    if (newDay) {
      Component dayText = getNewDayText(day, currentMonth, year);
      drawCenteredText(drawContext, dayText, width, (SEASON_OVERLAY_SCALE * 2), getColor(dayTime));

      if (holiday != null) {
        Component holidayText = getHolidayText(holiday);
        drawCenteredText(drawContext, holidayText, width, (SEASON_OVERLAY_SCALE * 3), getColor(dayTime));
      }

      if (currentMonth.getSubSeason().getSeason().isNewSeason(day, currentMonth)) {
        Component seasonText = getNewSeasonText(currentMonth);
        drawCenteredText(drawContext, seasonText, width, (SEASON_OVERLAY_SCALE * 3), getColor(dayTime));
      }
    }
  }

  /** A blit needs to name its pipeline now; the badge is a plain textured quad. */
  private void blitBadge(GuiGraphicsExtractor drawContext, net.minecraft.resources.Identifier texture, int x, int y) {
    drawContext.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
            SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE);
  }

  /**
   * Centred by hand rather than with centeredText, which draws without a shadow. The message sits
   * over open world and needs the shadow to stay readable.
   */
  private void drawCenteredText(GuiGraphicsExtractor drawContext, Component text, int x, int y, int color) {
    Font textRenderer = Minecraft.getInstance().font;
    int textWidth = textRenderer.width(text);

    drawContext.text(textRenderer, text, getHalf(x) - getHalf(textWidth), y, color, true);
  }

  private Component getNewDayText(int day, Months currentMonth, int year) {
    // Example: Day 1 of January, Year 1
    return Component.nullToEmpty("Day " + day + " of " + currentMonth.getName() + ", Year " + year);
  }

  private Component getNewSeasonText(Months currentMonth) {
    return switch (currentMonth.getSubSeason().getSeason()) {
      case SPRING -> Component.nullToEmpty("The ice melted and the flowers starts to blossom...");
      case SUMMER -> Component.nullToEmpty("The sun starts to shine brighter...");
      case AUTUMN -> Component.nullToEmpty("The leaves begins to paint the ground...");
      case WINTER -> Component.nullToEmpty("The cold breeze has finally arrived... Hello, Winter!");
    };
  }

  private Component getHolidayText(Holiday holiday) {
    return switch (holiday) {
      case CHRISTMAS -> Component.nullToEmpty("Merry Christmas!");
      case HALLOWEEN -> Component.nullToEmpty("Trick or threat? Happy Halloween!");
    };
  }

  private int getHalf(int value) {
    return value / 2;
  }

  private boolean isNewDay(long dayTime) {
    long tickOfDay = dayTime % DAY_DURATION;

    return tickOfDay >= 0 && tickOfDay <= MESSAGE_DURATION;
  }

  /**
   * Alpha ramp for the day change message: fade in, hold at full opacity, fade out.
   *
   * <p>The alpha floor is deliberate. TextRenderer treats a colour whose alpha is below 0x04 as
   * "no alpha given" and forces it to fully opaque, so fading all the way to zero would flash the
   * text at full brightness on the first and last tick.
   */
  public int getColor(long dayTime) {
    int tickOfDay = (int) (dayTime % DAY_DURATION);

    if (tickOfDay < 0 || tickOfDay > MESSAGE_DURATION) {
      return 0;
    }

    int ticksFromEdge = Math.min(tickOfDay, MESSAGE_DURATION - tickOfDay);
    float fade = Math.min(1.0f, (float) ticksFromEdge / MESSAGE_FADE_DURATION);

    int alpha = MIN_TEXT_ALPHA + Math.round((MAX_TEXT_ALPHA - MIN_TEXT_ALPHA) * fade);

    return (alpha << 24) | 0xFFFFFF;
  }
}