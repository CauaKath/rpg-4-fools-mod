package net.abakath.rpg4fools.models;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.minecraft.nbt.NbtCompound;

public class DayData {
  private final int year;
  private final Months month;
  private final int day;
  private final boolean newDay;

  public DayData(int year, Months month, int day, boolean newDay) {
    this.year = year;
    this.month = month;
    this.day = day;
    this.newDay = newDay;
  }

  public DayData(int year, int month, int day, boolean newDay) {
    this.year = year;
    this.month = Months.values()[month];
    this.day = day;
    this.newDay = newDay;
  }

  public int getYear() {
    return year;
  }

  public Months getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  public boolean isNewDay() {
    return newDay;
  }

  public static void setPlayerDayData(IEntityDataSaver player, DayData dayData) {
    NbtCompound nbt = player.getPersistentData();
    nbt.putInt("rpg4fools.year", dayData.getYear());
    nbt.putInt("rpg4fools.month", dayData.getMonth().ordinal());
    nbt.putInt("rpg4fools.day", dayData.getDay());
    nbt.putBoolean("rpg4fools.newDay", dayData.isNewDay());
  }

}
