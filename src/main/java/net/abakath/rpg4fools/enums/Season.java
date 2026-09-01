package net.abakath.rpg4fools.enums;

import net.abakath.rpg4fools.RPG4Fools;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

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
            case SPRING -> Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "textures/gui/spring.png");
            case SUMMER -> Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "textures/gui/summer.png");
            case AUTUMN -> Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "textures/gui/autumn.png");
            case WINTER -> Identifier.fromNamespaceAndPath(RPG4Fools.MOD_ID, "textures/gui/winter.png");
        };
    }

    /**
     * Colour the season is written in, for anything that names a season in text.
     *
     * <p>Vanilla formatting codes rather than RGB, so the colours follow whatever the player's
     * client already does with them and stay readable against every tooltip background.
     */
    public ChatFormatting getColor() {
        return switch (this) {
            case SPRING -> ChatFormatting.GREEN;
            case SUMMER -> ChatFormatting.YELLOW;
            case AUTUMN -> ChatFormatting.GOLD;
            case WINTER -> ChatFormatting.AQUA;
        };
    }

    public boolean isNewSeason(int day, Months month) {
        return switch (this) {
            case SPRING -> month == Months.JANUARY && day == 1;
            case SUMMER -> month == Months.APRIL && day == 1;
            case AUTUMN -> month == Months.JULY && day == 1;
            case WINTER -> month == Months.OCTOBER && day == 1;
        };
    }
}
