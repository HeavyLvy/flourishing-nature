package io.github.heavylvy.flourishingnature.core.predator;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;

/** What a wild wolf is willing to hunt right now. */
public enum Diet {
    /** Fed or full: leaves animals alone. */
    NOT_HUNTING,
    /** Hungry: vanilla wolf prey (sheep, rabbits, foxes, baby turtles). */
    USUAL_PREY,
    /** Starving: also other herd grazers (chickens, pigs, cows, goats). */
    ANY_PREY;

    public static Diet of(HungerBand hunger) {
        return switch (hunger) {
            case FULL, FED -> NOT_HUNTING;
            case HUNGRY -> USUAL_PREY;
            case STARVING -> ANY_PREY;
        };
    }

    public boolean hunts() {
        return this != NOT_HUNTING;
    }
}
