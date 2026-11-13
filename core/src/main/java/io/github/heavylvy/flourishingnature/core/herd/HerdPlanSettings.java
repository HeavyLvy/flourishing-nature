package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Tuning for herd-level decisions. Distances in blocks, times in ticks.
 *
 * @param joinRadius an unassigned animal joins a herd whose home is this close
 * @param mergeRadius herds whose homes are this close may merge
 * @param lonelyMergeRadius herds well under their wanted size look this far for a merge
 * @param transferRadius a crowded animal looks this far for a roomier herd
 * @param leashRadius an animal this far from its herd's home has lost the herd and leaves it
 * @param splitDistance a splinter herd sets its new home about this far away
 * @param crowdFactor a herd is crowded when bigger than this times its members' average wish
 * @param minAffinity species must like each other at least this much to share a herd
 * @param roamChance share of herds that roam
 * @param roamDistance how far a roaming herd moves its home each time
 * @param roamInterval ticks between moves for roaming herds (randomised up to double)
 * @param driftDistance how far a settled herd shuffles its home
 * @param driftInterval ticks between shuffles for settled herds (randomised up to double)
 * @param herdSpacing herds of species that get along, whose homes are closer than this, drift apart
 * @param spacingStep how far each such home moves away per planning pass (every 2 seconds)
 */
public record HerdPlanSettings(
        double joinRadius,
        double mergeRadius,
        double lonelyMergeRadius,
        double transferRadius,
        double leashRadius,
        double splitDistance,
        double crowdFactor,
        double minAffinity,
        double roamChance,
        double roamDistance,
        long roamInterval,
        double driftDistance,
        long driftInterval,
        double herdSpacing,
        double spacingStep) {

    public static final HerdPlanSettings DEFAULT =
            new HerdPlanSettings(16, 10, 20, 24, 40, 14, 1.5, 0.3, 0.35, 18, 1200, 4, 2400, 24, 1.0);

    public HerdPlanSettings {
        if (!(crowdFactor > 1.0) || !(minAffinity > 0.0) || !(roamChance >= 0.0 && roamChance <= 1.0)
                || roamInterval < 1 || driftInterval < 1 || !(leashRadius > joinRadius)
                || !(herdSpacing > mergeRadius) || !(spacingStep > 0.0)) {
            throw new IllegalArgumentException("bad herd plan settings");
        }
    }
}
