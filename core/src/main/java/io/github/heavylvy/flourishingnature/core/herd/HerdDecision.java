package io.github.heavylvy.flourishingnature.core.herd;

/** What a mob should do about its herd right now. */
public sealed interface HerdDecision {

    /** Herd is about the right size. Cohesion keeps it together. */
    record Content() implements HerdDecision {}

    /**
     * Too few mates: walk toward the best group nearby.
     *
     * @param target centre of the chosen group
     */
    record SeekHerd(Position target) implements HerdDecision {}

    /**
     * Too many mates: drift toward the edge, away from the crowd.
     *
     * @param target a point away from the crowd's centre
     */
    record Disperse(Position target) implements HerdDecision {}

    /**
     * Wants more company but no better group is in sight, so it keeps the mates it has (or stays
     * alone).
     */
    record Lonely() implements HerdDecision {}
}
