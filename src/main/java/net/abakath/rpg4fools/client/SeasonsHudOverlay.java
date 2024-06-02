package net.abakath.rpg4fools.client;

import net.abakath.rpg4fools.RPG4Fools;
import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.models.Scale;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SeasonsHudOverlay implements HudRenderCallback {
  private static final Identifier DAY_CYCLE = new Identifier(RPG4Fools.MOD_ID, "textures/gui/cycle.png");

  @Override
  public void onHudRender(DrawContext drawContext, float tickDelta) {
    int x = 0;
    Scale defaultScale = new Scale(40, 32);

    MinecraftClient client = MinecraftClient.getInstance();
    if (client != null) {
      int width = client.getWindow().getScaledWidth();

      Integer guiScale = client.options.getGuiScale().getValue();
      defaultScale = this.getScale(guiScale);
      x = width - defaultScale.getWidth() - 8;
    }

    int widthScale = defaultScale.getWidth();
    int heightScale = defaultScale.getHeight();

    assert client != null;
    assert client.player != null;
    int year = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.year");
    int month = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.month");
    int day = ((IEntityDataSaver) client.player).getPersistentData().getInt("rpg4fools.day");

//    drawContext.drawTexture(DAY_CYCLE, x, 8, 0, 0, widthScale, heightScale, widthScale, heightScale);
    drawContext.drawText(client.textRenderer, "Year: " + year, x + 4, 8, 0xFFFFFF, true);
    drawContext.drawText(client.textRenderer, "Month: " + Months.values()[month].getName(), x + 4, 16, 0xFFFFFF, true);
    drawContext.drawText(client.textRenderer, "Day: " + day, x + 4, 24, 0xFFFFFF, true);
    drawContext.drawText(client.textRenderer, "Season: " + Months.values()[month].getSeason().getName(), x + 4, 32, 0xFFFFFF, true);
  }


  private Scale getScale(Integer guiScale) {
    Map<Integer, Scale> scaleMap = new HashMap<>();

    // TODO: Adjust scales
    scaleMap.put(0, new Scale(40, 32));
    scaleMap.put(1, new Scale(60, 48));
    scaleMap.put(2, new Scale(80, 64));
    scaleMap.put(3, new Scale(100, 80));
    scaleMap.put(4, new Scale(120, 96));

    return scaleMap.get(guiScale);
  }
}