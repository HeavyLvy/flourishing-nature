package io.github.heavylvy.flourishingnature.core.jobs;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;

/**
 * When a resting cow leaves a pat. Only a fed (FULL or FED) and happy (CONTENT or THRIVING) cow
 * that isn't crammed makes one; the rate then scales like any other product.
 */
public final class CowPatRules {
    private CowPatRules() {}

    public static boolean mayLeavePat(HungerBand hunger, Mood mood, boolean crammed) {
        boolean fed = switch (hunger) {
            case FULL, FED -> true;
            case HUNGRY, STARVING -> false;
        };
        boolean happy = switch (mood) {
            case THRIVING, CONTENT -> true;
            case STRESSED, MISERABLE -> false;
        };
        return fed && happy && !crammed;
    }

    /** Chance per check: the base rate scaled by {@link ProductionRules#productRate}, or 0. */
    public static double chancePerCheck(HungerBand hunger, Mood mood, boolean crammed, double patsPerDay, long intervalTicks) {
        if (!mayLeavePat(hunger, mood, crammed)) {
            return 0.0;
        }
        return Math.min(1.0, JobChance.perCheck(patsPerDay, intervalTicks) * ProductionRules.productRate(mood, hunger));
    }
}
