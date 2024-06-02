package net.abakath.rpg4fools.enums;

public enum Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    public String getName() {
        return switch (this) {
            case SPRING -> "Spring";
            case SUMMER -> "Summer";
            case AUTUMN -> "Autumn";
            case WINTER -> "Winter";
        };
    }
}
