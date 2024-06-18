package net.abakath.rpg4fools.models;

import net.abakath.rpg4fools.enums.Months;
import net.abakath.rpg4fools.utils.IEntityDataSaver;
import net.minecraft.nbt.NbtCompound;

public class DayData {
  private final int year;
  private final Months month;
  private final int day;
  private final long dayTime;

  public DayData(int year, Months month, int day, long newDay) {
    this.year = year;
    this.month = month;
    this.day = day;
    this.dayTime = newDay;
  }

  public DayData(int year, int month, int day, long newDay) {
    this.year = year;
    this.month = Months.values()[month];
    this.day = day;
    this.dayTime = newDay;
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

  public long getDayTime() {
    return dayTime;
  }

  public static void setPlayerDayData(IEntityDataSaver player, DayData dayData) {
    NbtCompound nbt = player.getPersistentData();
    nbt.putInt("rpg4fools.year", dayData.getYear());
    nbt.putInt("rpg4fools.month", dayData.getMonth().ordinal());
    nbt.putInt("rpg4fools.day", dayData.getDay());
    nbt.putLong("rpg4fools.dayTime", dayData.getDayTime());
  }

}
