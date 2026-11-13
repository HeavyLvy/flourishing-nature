package io.github.heavylvy.flourishingnature.core.herd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Light flocking inside a herd: keep a comfortable gap to the nearest mate, and close up when the
 * herd drifts off.
 */
public final class Cohesion {
    private Cohesion() {}

    /** Where to step to stay with the herd, or empty if the spacing is already comfortable. */
    public static Optional<Position> adjust(HerdMember self, List<Neighbor> neighbors, HerdingSettings settings) {
        double herdSquared = settings.herdRadius() * settings.herdRadius();
        List<Position> mates = new ArrayList<>();
        Optional<Position> nearest = Optional.empty();
        double nearestSquared = Double.MAX_VALUE;
        for (Neighbor n : neighbors) {
            double d = n.position().distanceSquaredTo(self.position());
            if (d > herdSquared || SocialWeight.between(self, n, settings) <= 0.0) {
                continue;
            }
            mates.add(n.position());
            if (d < nearestSquared) {
                nearestSquared = d;
                nearest = Optional.of(n.position());
            }
        }
        double nearestDistance = Math.sqrt(nearestSquared);
        return nearest.flatMap(closest -> {
            if (nearestDistance < settings.minSpacing()) {
                return Optional.of(self.position().awayFrom(closest, settings.minSpacing()));
            }
            if (nearestDistance > settings.maxSpacing()) {
                return Optional.of(Position.centroid(mates));
            }
            return Optional.empty();
        });
    }
}
