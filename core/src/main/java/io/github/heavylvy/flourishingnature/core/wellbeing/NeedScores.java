package io.github.heavylvy.flourishingnature.core.wellbeing;

import java.util.EnumMap;
import java.util.Map;

/**
 * A score for every need.
 *
 * @param scores one entry per {@link Need}
 */
public record NeedScores(Map<Need, NeedScore> scores) {
    public NeedScores {
        for (Need need : Need.values()) {
            if (!scores.containsKey(need)) {
                throw new IllegalArgumentException("missing score for " + need);
            }
        }
        scores = Map.copyOf(new EnumMap<>(scores));
    }

    public NeedScore of(Need need) {
        return scores.getOrDefault(need, NeedScore.EMPTY);
    }

    /** The weighted average, scaled to 0 to 100: the wellbeing this animal is heading toward. */
    public Wellbeing target(NeedWeights weights) {
        double weighted = 0.0;
        double total = 0.0;
        for (Need need : Need.values()) {
            weighted += weights.of(need) * of(need).value();
            total += weights.of(need);
        }
        return new Wellbeing(Math.max(0.0, Math.min(Wellbeing.MAX, Wellbeing.MAX * weighted / total)));
    }
}
