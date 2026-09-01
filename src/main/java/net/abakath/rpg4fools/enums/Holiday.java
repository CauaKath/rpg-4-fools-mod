package net.abakath.rpg4fools.enums;

import net.minecraft.resources.ResourceLocation;

public enum Holiday {
  CHRISTMAS("Christmas", 25, 12),
  HALLOWEEN("Halloween", 28, 10);

  private final String name;
  private final int day;
  private final int month;

  Holiday(String name, int day, int month) {
    this.name = name;
    this.day = day;
    this.month = month;
  }

  public String getName() {
    return name;
  }

  public ResourceLocation getHolidayTexture() {
    return switch (this) {
      case CHRISTMAS -> new ResourceLocation("rpg4fools", "textures/gui/christmas.png");
      case HALLOWEEN -> new ResourceLocation("rpg4fools", "textures/gui/halloween.png");
    };
  }

  public static Holiday getHoliday(int day, int month) {
    for (Holiday holiday : values()) {
      if (holiday.day == day && holiday.month == month) {
        return holiday;
      }
    }
    return null;
  }
}
