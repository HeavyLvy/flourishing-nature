package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * Per-species wellbeing tuning.
 *
 * @param weights how much each need matters
 * @param comfortLimit mobs within close quarters before space starts to suffer (cow: 4)
 * @param foodForFullScore food blocks nearby that count as a full pasture
 */
public record WellbeingProfile(NeedWeights weights, int comfortLimit, int foodForFullScore) {
    public WellbeingProfile {
        if (comfortLimit < 1 || foodForFullScore < 1) {
            throw new IllegalArgumentException("comfort limit and food target must be at least 1");
        }
    }

    public static WellbeingProfile grazer() {
        return new WellbeingProfile(NeedWeights.grazer(), 4, 16);
    }
}
