package io.github.heavylvy.flourishingnature.core.fullness;

/**
 * What fullness means for the animal (design doc, "Hunger without death"). Starving animals never
 * die; they just stop giving you things.
 */
public enum HungerBand {
    /** 0.7 and up: can breed on its own; products and drops normal, scaled by wellbeing. */
    FULL,
    /** 0.4 to 0.7: normal products, no self-breeding. */
    FED,
    /** 0.15 to 0.4: no breeding, products at half rate, drops one roll fewer. */
    HUNGRY,
    /** Under 0.15: no breeding, no milk or eggs, at most 1 wool, minimum drops. */
    STARVING;

    public static final double FULL_AT = 0.7;
    public static final double FED_AT = 0.4;
    public static final double HUNGRY_AT = 0.15;

    public static HungerBand of(Fullness f) {
        double v = f.value();
        if (v >= FULL_AT) {
            return FULL;
        }
        if (v >= FED_AT) {
            return FED;
        }
        if (v >= HUNGRY_AT) {
            return HUNGRY;
        }
        return STARVING;
    }
}
