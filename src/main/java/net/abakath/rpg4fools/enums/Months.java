package net.abakath.rpg4fools.enums;

public enum Months {
    JANUARY(Season.SUMMER, "January"),
    FEBRUARY(Season.SUMMER, "February"),
    MARCH(Season.SUMMER, "March"),
    APRIL(Season.AUTUMN, "April"),
    MAY(Season.AUTUMN, "May"),
    JUNE(Season.AUTUMN, "June"),
    JULY(Season.WINTER, "July"),
    AUGUST(Season.WINTER, "August"),
    SEPTEMBER(Season.WINTER, "September"),
    OCTOBER(Season.SPRING, "October"),
    NOVEMBER(Season.SPRING, "November"),
    DECEMBER(Season.SPRING, "December");

    private final Season season;
    private final String name;

    Months(Season season, String name) {
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
