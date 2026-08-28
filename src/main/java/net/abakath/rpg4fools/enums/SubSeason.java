package net.abakath.rpg4fools.enums;

public enum SubSeason {
  EARLY_SPRING(Season.SPRING, "Early Spring"),
  MID_SPRING(Season.SPRING, "Mid Spring"),
  LATE_SPRING(Season.SPRING, "Late Spring"),
  EARLY_SUMMER(Season.SUMMER, "Early Summer"),
  MID_SUMMER(Season.SUMMER, "Mid Summer"),
  LATE_SUMMER(Season.SUMMER, "Late Summer"),
  EARLY_AUTUMN(Season.AUTUMN, "Early Autumn"),
  MID_AUTUMN(Season.AUTUMN, "Mid Autumn"),
  LATE_AUTUMN(Season.AUTUMN, "Late Autumn"),
  EARLY_WINTER(Season.WINTER, "Early Winter"),
  MID_WINTER(Season.WINTER, "Mid Winter"),
  LATE_WINTER(Season.WINTER, "Late Winter");

  private final Season season;
  private final String name;

  SubSeason(Season season, String name) {
    this.season = season;
    this.name = name;
  }

  public Season getSeason() {
    return season;
  }

  public String getName() {
    return name;
  }
}
