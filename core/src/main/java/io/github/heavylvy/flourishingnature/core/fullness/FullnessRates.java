package io.github.heavylvy.flourishingnature.core.fullness;

import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;

/**
 * How fast each kind of animal gets hungry, and how much food is worth. One in-game day is 24,000
 * ticks.
 */
public final class FullnessRates {
    private FullnessRates() {}

    public static final long DAY = 24_000L;

    /** Grazers go from full to empty in about two in-game days without food. */
    public static final double GRAZER_DRAIN = 1.0 / (2 * DAY);
    /** A kill keeps a wolf fed about two days (from full down to hungry). */
    public static final double PREDATOR_DRAIN = (1.0 - HungerBand.FED_AT) / (2 * DAY);

    /** One grass plant, flower or crop eaten from the world. */
    public static final double PLANT_MEAL = 0.08;
    /** A grass block cropped to dirt (sheep). */
    public static final double GRASS_BLOCK_MEAL = 0.06;
    /** Hand-fed food: a starving animal recovers in a few bites. */
    public static final double HAND_FED = 0.35;

    public static double drainPerTick(SpeciesId species) {
        return species.equals(SpeciesId.minecraft("wolf")) ? PREDATOR_DRAIN : GRAZER_DRAIN;
    }
}
