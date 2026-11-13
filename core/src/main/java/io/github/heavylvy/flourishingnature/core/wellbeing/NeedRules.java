package io.github.heavylvy.flourishingnature.core.wellbeing;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import java.util.EnumMap;
import java.util.Map;

/** Turns what an animal noticed into a 0 to 1 score per need. */
public final class NeedRules {
    private NeedRules() {}

    /** Being dry counts as sheltered; only rain without a roof in reach is a "no". */
    static boolean shelterSample(Surroundings s) {
        return !s.precipitating() || s.roofNearby();
    }

    public static NeedScores score(
            Surroundings s, RollingMemory sky, RollingMemory shelter, WellbeingProfile profile, WellbeingSettings settings) {
        Map<Need, NeedScore> scores = new EnumMap<>(Need.class);
        scores.put(Need.BIOME, s.biome().score());
        scores.put(Need.SKY, new NeedScore(sky.value()));
        scores.put(Need.SHELTER, shelterScore(shelter, settings));
        scores.put(Need.FOOD, food(s, profile, settings));
        scores.put(Need.SPACE, space(s.mobsCloseBy(), profile.comfortLimit(), settings.crammingLimit()));
        scores.put(Need.SOCIAL, social(s.socialCount(), s.preference().value()));
        return new NeedScores(scores);
    }

    /** Memory of 1 means always dry; 0 means always rained on with no roof. */
    static NeedScore shelterScore(RollingMemory shelter, WellbeingSettings settings) {
        double exposed = settings.exposedShelterScore();
        return NeedScore.clamped(exposed + (1.0 - exposed) * shelter.value());
    }

    /**
     * How fed it is and how much it could eat nearby, half each; a recent hand-fed meal can lift it.
     * Being full counts in full from {@link HungerBand#FULL_AT}. An animal that doesn't eat plants
     * is scored on fullness alone.
     */
    static NeedScore food(Surroundings s, WellbeingProfile profile, WellbeingSettings settings) {
        double fed = Math.min(1.0, s.fullness().value() / HungerBand.FULL_AT);
        double plants = Math.min(1.0, (double) s.ediblePlantsNearby() / profile.foodForFullScore());
        double now = s.eatsPlants() ? 0.5 * fed + 0.5 * plants : fed;
        double meal = Math.max(0.0, 1.0 - (double) s.ticksSinceMeal() / settings.mealMemoryTicks());
        return NeedScore.clamped(Math.max(now, meal));
    }

    /**
     * Full up to the comfort limit, then falls fast, reaching 0 at twice the limit or at the
     * cramming limit, whichever comes first.
     */
    static NeedScore space(int mobsCloseBy, int comfortLimit, int crammingLimit) {
        if (mobsCloseBy >= crammingLimit - 1) {
            return NeedScore.EMPTY;
        }
        if (mobsCloseBy <= comfortLimit) {
            return NeedScore.FULL;
        }
        return NeedScore.clamped(1.0 - (double) (mobsCloseBy - comfortLimit) / comfortLimit);
    }

    /**
     * 1 when the herd it feels it has is within one of the size it wants, falling off on either
     * side. Loners are happy alone.
     */
    static NeedScore social(double socialCount, int preference) {
        double felt = 1.0 + socialCount;
        double miss = Math.abs(felt - preference);
        if (miss <= 1.0) {
            return NeedScore.FULL;
        }
        return NeedScore.clamped(1.0 - (miss - 1.0) / Math.max(2.0, preference));
    }
}
