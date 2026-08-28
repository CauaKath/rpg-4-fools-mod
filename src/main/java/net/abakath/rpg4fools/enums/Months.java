package net.abakath.rpg4fools.enums;

public enum Months {
    JANUARY(SubSeason.EARLY_SPRING, "January"),
    FEBRUARY(SubSeason.MID_SPRING, "February"),
    MARCH(SubSeason.LATE_SPRING, "March"),
    APRIL(SubSeason.EARLY_SUMMER, "April"),
    MAY(SubSeason.MID_SUMMER, "May"),
    JUNE(SubSeason.LATE_SUMMER, "June"),
    JULY(SubSeason.EARLY_AUTUMN, "July"),
    AUGUST(SubSeason.MID_AUTUMN, "August"),
    SEPTEMBER(SubSeason.LATE_AUTUMN, "September"),
    OCTOBER(SubSeason.EARLY_WINTER, "October"),
    NOVEMBER(SubSeason.MID_WINTER, "November"),
    DECEMBER(SubSeason.LATE_WINTER, "December");

    private final SubSeason season;
    private final String name;

    Months(SubSeason season, String name) {
        this.season = season;
        this.name = name;
    }

    public SubSeason getSubSeason() {
        return season;
    }

    public String getName() {
        return name;
    }
}
