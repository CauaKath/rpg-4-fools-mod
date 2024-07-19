package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Holiday;
import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SeasonsHudOverlay implements HudRenderCallback {
  private static final int SEASON_OVERLAY_SCALE = 16;

  @Override
  public void onHudRender(DrawContext drawContext, float tickDelta) {
    MinecraftClient client = MinecraftClient.getInstance();

    assert client != null;
    assert client.player != null;
    if (client.player.isInCreativeMode() || client.player.isSpectator()) {
      return;
    }

    assert client.world != null;
    if (!client.world.getRegistryKey().equals(World.OVERWORLD)) {
      return;
    }

    int width = client.getWindow().getScaledWidth();
    int height = client.getWindow().getScaledHeight();

    int x = getHalf(width) - getHalf(SEASON_OVERLAY_SCALE);
    int y = height - (SEASON_OVERLAY_SCALE * 3);

    IEntityDataSaver playerData = (IEntityDataSaver) client.player;

    int year = playerData.getPersistentData().getInt("rpg4fools.year");
    int month = playerData.getPersistentData().getInt("rpg4fools.month");
    int day = playerData.getPersistentData().getInt("rpg4fools.day");
    long dayTime = playerData.getPersistentData().getLong("rpg4fools.dayTime");

    Months currentMonth = Months.values()[month];
    Holiday holiday = Holiday.getHoliday(day, (month + 1));

    if (holiday != null) {
      drawContext.drawTexture(holiday.getHolidayTexture(), x, y, 0, 0, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE);
    } else {
      drawContext.drawTexture(currentMonth.getSeason().getSeasonTexture(), x, y, 0, 0, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE, SEASON_OVERLAY_SCALE);
    }

    boolean newDay = isNewDay(dayTime);

    if (newDay) {
      Text dayText = getNewDayText(day, currentMonth, year);
      drawCenteredText(drawContext, dayText, width, (SEASON_OVERLAY_SCALE * 2), getColor(dayTime));

      if (holiday != null) {
        Text holidayText = getHolidayText(holiday);
        drawCenteredText(drawContext, holidayText, width, (SEASON_OVERLAY_SCALE * 3), getColor(dayTime));
      }

      if (currentMonth.getSeason().isNewSeason(day, currentMonth)) {
        Text seasonText = getNewSeasonText(currentMonth);
        drawCenteredText(drawContext, seasonText, width, (SEASON_OVERLAY_SCALE * 3), getColor(dayTime));
      }
    }
  }

  private void drawCenteredText(DrawContext drawContext, Text text, int x, int y, int color) {
    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    int textWidth = textRenderer.getWidth(text);

    drawContext.drawText(textRenderer, text, getHalf(x) - getHalf(textWidth), y, color, true);
  }

  private Text getNewDayText(int day, Months currentMonth, int year) {
    // Example: Day 1 of January, Year 1
    return Text.of("Day " + day + " of " + currentMonth.getName() + ", Year " + year);
  }

  private Text getNewSeasonText(Months currentMonth) {
    return switch (currentMonth.getSeason()) {
      case SPRING -> Text.of("The ice melted and the flowers starts to blossom...");
      case SUMMER -> Text.of("The sun starts to shine brighter...");
      case AUTUMN -> Text.of("The leaves begins to paint the ground...");
      case WINTER -> Text.of("The cold breeze has finally arrived... Hello, Winter!");
    };
  }

  private Text getHolidayText(Holiday holiday) {
    return switch (holiday) {
      case CHRISTMAS -> Text.of("Merry Christmas!");
      case HALLOWEEN -> Text.of("Trick or threat? Happy Halloween!");
    };
  }

  private int getHalf(int value) {
    return value / 2;
  }

  private boolean isNewDay(long dayTime) {
    long calc = dayTime % 24000;

    return calc >= 0 && calc <= 48;
  }

  public int getColor(long dayTime) {
    return switch ((int) (dayTime % 24000)) {
      case 0, 1, 2, 46, 47, 48 -> 0x20FFFFFF;
      case 3, 4, 5, 43, 44, 45 -> 0x40FFFFFF;
      case 6, 7, 8, 40, 41, 42 -> 0x60FFFFFF;
      case 9, 10, 11, 37, 38, 39 -> 0x80FFFFFF;
      case 12, 13, 14, 34, 35, 36 -> 0xA0FFFFFF;
      case 15, 16, 17, 31, 32, 33 -> 0xC0FFFFFF;
      case 18, 19, 20, 28, 29, 30 -> 0xD0FFFFFF;
      case 21, 22, 26, 27 -> 0xE0FFFFFF;
      case 23, 24, 25 -> 0xF0FFFFFF;

      default -> 0x000000;
    };
  }
}