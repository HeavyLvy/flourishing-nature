package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;

/**
 * Breeding without a player: a full, happy adult next to a herd mate in the same state falls in
 * love on its own, then waits a few days.
 */
public final class SelfBreeding {
    private SelfBreeding() {}

    /** Why an animal will or won't breed on its own right now. */
    public enum Verdict {
        READY,
        /** A baby, already in love, or still on vanilla's breeding cooldown. */
        NOT_READY_TO_LOVE,
        /** Not full, or not content or thriving ({@link ProductionRules#canSelfBreed}). */
        NOT_WELL_ENOUGH,
        /** Bred on its own too recently. */
        COOLING_DOWN,
        /** Too many of its kind nearby (the soft population cap). */
        CROWDED
    }

    /**
     * Whether an animal may fall in love on its own now, and if not, why.
     *
     * @param canFallInLove adult, not in love, off vanilla's breeding cooldown
     * @param lastSelfBred game tick it last bred on its own, or a very negative number if never
     * @param sameSpeciesNearby animals of its species in the cap area, counting itself
     */
    public static Verdict check(
            boolean canFallInLove,
            Mood mood,
            HungerBand hunger,
            long now,
            long lastSelfBred,
            int sameSpeciesNearby,
            int populationCap,
            SelfBreedingSettings settings) {
        if (!canFallInLove) {
            return Verdict.NOT_READY_TO_LOVE;
        }
        if (!ProductionRules.canSelfBreed(mood, hunger)) {
            return Verdict.NOT_WELL_ENOUGH;
        }
        if (now - lastSelfBred < settings.cooldownTicks()) {
            return Verdict.COOLING_DOWN;
        }
        if (sameSpeciesNearby >= populationCap) {
            return Verdict.CROWDED;
        }
        return Verdict.READY;
    }
}
