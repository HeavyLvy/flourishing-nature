package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * Global wellbeing tuning. Times are in game ticks (20 per second).
 *
 * @param checkInterval ticks between need checks (design: 100)
 * @param ticksPerPoint ticks for wellbeing to move one point toward its target (design: 600)
 * @param memoryWeight how much each new sky/shelter sample counts
 * @param mealMemoryTicks how long a meal keeps the food need up
 * @param crammingLimit mobs close by at which space scores 0 (vanilla maxEntityCramming)
 * @param exposedShelterScore shelter score when it's raining and no roof is in reach
 */
public record WellbeingSettings(
        int checkInterval,
        int ticksPerPoint,
        double memoryWeight,
        long mealMemoryTicks,
        int crammingLimit,
        double exposedShelterScore) {

    public static final WellbeingSettings DEFAULT = new WellbeingSettings(100, 600, 0.15, 24_000L, 24, 0.2);

    public WellbeingSettings {
        if (checkInterval < 1 || ticksPerPoint < 1 || mealMemoryTicks < 1 || crammingLimit < 2) {
            throw new IllegalArgumentException("wellbeing intervals and limits must be positive");
        }
        if (!(memoryWeight > 0.0 && memoryWeight <= 1.0)) {
            throw new IllegalArgumentException("memoryWeight must be in (0, 1]: " + memoryWeight);
        }
        if (!(exposedShelterScore >= 0.0 && exposedShelterScore <= 1.0)) {
            throw new IllegalArgumentException("exposedShelterScore must be in [0, 1]");
        }
    }

    /** Most wellbeing can change in one check. */
    public double maxStepPerCheck() {
        return (double) checkInterval / ticksPerPoint;
    }
}
