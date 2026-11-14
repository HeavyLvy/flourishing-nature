package io.github.heavylvy.flourishingnature.core.predator;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import java.util.List;
import java.util.Optional;

/**
 * Hunters go for the weak and the young first. Distance still counts, so a healthy sheep next to
 * the wolf beats a lamb on the far side of the valley, but a lamb or a limping animal is worth a
 * longer chase: the effective distance is divided by {@code 1 + 2 * weakness}, so a baby counts
 * as a third as far away as a healthy adult.
 */
public final class PreyChoice {
    private PreyChoice() {}

    static final double WEAKNESS_PULL = 2.0;

    public static Optional<Prey> pick(Position hunter, List<Prey> candidates, double maxDistance) {
        Optional<Prey> best = Optional.empty();
        double bestScore = Double.POSITIVE_INFINITY;
        for (Prey prey : candidates) {
            double distance = prey.position().distanceTo(hunter);
            if (distance > maxDistance) {
                continue;
            }
            double score = distance / (1.0 + WEAKNESS_PULL * prey.weakness());
            if (score < bestScore) {
                bestScore = score;
                best = Optional.of(prey);
            }
        }
        return best;
    }
}
