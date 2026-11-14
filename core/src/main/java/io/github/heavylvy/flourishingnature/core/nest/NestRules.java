package io.github.heavylvy.flourishingnature.core.nest;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Decisions about nests that don't need the world: decay, brooding, where to build. */
public final class NestRules {
    private NestRules() {}

    /** Sticks a decayed wild nest leaves behind. */
    public static final int DECAY_STICKS = 3;

    /** Crafted nests last forever; a wild nest with no hen seen for long enough falls apart. */
    public static boolean decays(boolean crafted, long lastHenSeen, long now, NestSettings settings) {
        return !crafted && now - lastHenSeen >= settings.wildDecayTicks();
    }

    /** Whether a free hen starts a sitting bout now. Only fertile eggs are worth sitting on. */
    public static boolean startsBrooding(boolean fertileEggs, Randomness random, NestSettings settings) {
        return fertileEggs && random.nextDouble() < settings.broodShare();
    }

    /**
     * A spot a nest could go.
     *
     * @param at the spot
     * @param covered whether something overhead hides it from the sky
     */
    public record Site(Position at, boolean covered) {}

    /** Prefers covered spots, then the one closest to the herd's home. */
    public static Optional<Site> bestSite(List<Site> candidates, Position home) {
        return candidates.stream().min(Comparator
                .comparing((Site site) -> !site.covered())
                .thenComparingDouble(site -> site.at().distanceSquaredTo(home)));
    }
}
