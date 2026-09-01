package net.abakath.rpg4fools.models;

import net.abakath.rpg4fools.enums.Months;

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

}
