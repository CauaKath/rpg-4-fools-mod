package net.abakath.rpg4fools.enums;

import net.abakath.rpg4fools.RPG4Fools;
import net.minecraft.util.Identifier;

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

    public Identifier getSeasonTexture() {
        return switch (this) {
            case SPRING -> new Identifier(RPG4Fools.MOD_ID, "textures/gui/spring.png");
            case SUMMER -> new Identifier(RPG4Fools.MOD_ID, "textures/gui/summer.png");
            case AUTUMN -> new Identifier(RPG4Fools.MOD_ID, "textures/gui/autumn.png");
            case WINTER -> new Identifier(RPG4Fools.MOD_ID, "textures/gui/winter.png");
        };
    }
}
