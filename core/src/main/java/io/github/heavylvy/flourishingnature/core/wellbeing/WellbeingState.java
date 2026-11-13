package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * Everything saved per animal for wellbeing.
 *
 * @param wellbeing current score
 * @param needs need scores from the last check
 * @param sky rolling memory of having sky access
 * @param shelter rolling memory of being sheltered when it rains
 */
public record WellbeingState(Wellbeing wellbeing, NeedScores needs, RollingMemory sky, RollingMemory shelter) {

    /** A freshly checked animal starts at its target instead of a made-up middle value. */
    public static WellbeingState first(Surroundings surroundings, WellbeingProfile profile, WellbeingSettings settings) {
        RollingMemory sky = new RollingMemory(surroundings.skyNearby() ? 1.0 : 0.0);
        RollingMemory shelter = new RollingMemory(NeedRules.shelterSample(surroundings) ? 1.0 : 0.0);
        NeedScores needs = NeedRules.score(surroundings, sky, shelter, profile, settings);
        return new WellbeingState(needs.target(profile.weights()), needs, sky, shelter);
    }

    /** One check later: update memories, rescore needs, and drift toward the new target. */
    public WellbeingState next(Surroundings surroundings, WellbeingProfile profile, WellbeingSettings settings) {
        RollingMemory newSky = sky.remember(surroundings.skyNearby(), settings.memoryWeight());
        RollingMemory newShelter = shelter.remember(NeedRules.shelterSample(surroundings), settings.memoryWeight());
        NeedScores newNeeds = NeedRules.score(surroundings, newSky, newShelter, profile, settings);
        Wellbeing drifted = wellbeing.approach(newNeeds.target(profile.weights()), settings.maxStepPerCheck());
        return new WellbeingState(drifted, newNeeds, newSky, newShelter);
    }
}
