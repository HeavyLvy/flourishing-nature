package io.github.heavylvy.flourishingnature.core.production;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;

/**
 * The design doc's tables, combined: wellbeing sets the rate, hunger caps it.
 *
 * <p>Wellbeing (System 2, "Effects of wellbeing"): thriving 2x, content 1x, stressed 1/3,
 * miserable 0. Hunger ("Hunger without death"): full and fed normal, hungry half, starving 0.
 */
public final class ProductionRules {
    private ProductionRules() {}

    /** Multiplier on product rates (milk refill, egg laying, wool regrowth). 0 means none. */
    public static double productRate(Mood mood, HungerBand hunger) {
        double byMood = switch (mood) {
            case THRIVING -> 2.0;
            case CONTENT -> 1.0;
            case STRESSED -> 1.0 / 3.0;
            case MISERABLE -> 0.0;
        };
        double byHunger = switch (hunger) {
            case FULL, FED -> 1.0;
            case HUNGRY -> 0.5;
            case STARVING -> 0.0;
        };
        return byMood * byHunger;
    }

    /** Extra (or fewer) loot rolls on death. Starving animals drop the minimum regardless. */
    public static int dropRollBonus(Mood mood, HungerBand hunger) {
        int byMood = switch (mood) {
            case THRIVING -> 1;
            case CONTENT -> 0;
            case STRESSED -> -1;
            case MISERABLE -> -2;
        };
        int byHunger = switch (hunger) {
            case FULL, FED -> 0;
            case HUNGRY -> -1;
            case STARVING -> -2;
        };
        return byMood + byHunger;
    }

    /** Starving or miserable animals drop only the minimum: 1 meat, nothing else. */
    public static boolean minimumDropsOnly(Mood mood, HungerBand hunger) {
        return hunger == HungerBand.STARVING || mood == Mood.MISERABLE;
    }

    /** Whether it may enter love mode at all (player-fed or self-breeding). */
    public static boolean canBreed(Mood mood, HungerBand hunger) {
        return mood != Mood.MISERABLE && (hunger == HungerBand.FULL || hunger == HungerBand.FED);
    }

    /** Whether it may breed on its own, without a player. */
    public static boolean canSelfBreed(Mood mood, HungerBand hunger) {
        return hunger == HungerBand.FULL && (mood == Mood.THRIVING || mood == Mood.CONTENT);
    }

    /** Most wool a sheep gives per shearing: starving sheep give at most 1. */
    public static int maxWool(HungerBand hunger, int vanillaMax) {
        return hunger == HungerBand.STARVING ? Math.min(1, vanillaMax) : vanillaMax;
    }
}
