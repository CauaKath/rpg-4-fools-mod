package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.enums.Holiday;
import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SeasonsHudOverlay implements HudRenderCallback {
  private static final int seasonOverlayScale = 16;

  @Override
  public void onHudRender(DrawContext drawContext, float tickDelta) {
    int x = 0;
    int y = 0;

    MinecraftClient client = MinecraftClient.getInstance();
    if (client != null) {
      int width = client.getWindow().getScaledWidth();
      int height = client.getWindow().getScaledHeight();

      x = (width / 2) - (seasonOverlayScale / 2);
      y = height - (seasonOverlayScale * 3);
    }

    assert client != null;
    assert client.player != null;
    int year = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.year");
    int month = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.month");
    int day = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.day");
    boolean newDay = ((IEntityDataSaver) client.player).getPersistentData().getBoolean("rpg4fools.newDay");

    Months currentMonth = Months.values()[month];
    Holiday holiday = Holiday.getHoliday(day, (month + 1));

    if (holiday != null) {
      drawContext.drawTexture(holiday.getHolidayTexture(), x, y, 0, 0, seasonOverlayScale, seasonOverlayScale, seasonOverlayScale, seasonOverlayScale);
    } else {
      drawContext.drawTexture(currentMonth.getSeason().getSeasonTexture(), x, y, 0, 0, seasonOverlayScale, seasonOverlayScale, seasonOverlayScale, seasonOverlayScale);
    }

    if (newDay) {
      // TODO: Add text transition animation
      drawContext.drawText(client.textRenderer, getNewDayText(day, currentMonth, year), x + (seasonOverlayScale * 4), y + 8, 0xFFFFFF, true);
    }

//    drawContext.drawText(client.textRenderer, "Year: " + year, x + (seasonOverlayScale * 4), 8, 0xFFFFFF, true);
//    drawContext.drawText(client.textRenderer, "Month: " + currentMonth.getName(), x + (seasonOverlayScale * 4), 16, 0xFFFFFF, true);
//    drawContext.drawText(client.textRenderer, "Day: " + day, x + (seasonOverlayScale * 4), 24, 0xFFFFFF, true);
//    drawContext.drawText(client.textRenderer, "Season: " + currentMonth.getSeason().getName(), x + (seasonOverlayScale * 4), 32, 0xFFFFFF, true);
  }

  private Text getNewDayText(int day, Months currentMonth, int year) {
    // Example: Day 1 of January, Year 1
    return Text.of("Day " + day + " of " + currentMonth.getName() + ", Year " + year);
  }
}