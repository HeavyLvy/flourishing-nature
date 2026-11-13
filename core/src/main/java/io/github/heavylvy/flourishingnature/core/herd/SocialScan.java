package io.github.heavylvy.flourishingnature.core.herd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decides whether a mob should join a herd, stay, or drift away. Think of a dance floor: nobody
 * assigns groups, each mob drifts toward mobs it likes until its circle feels the right size.
 */
public final class SocialScan {
    private SocialScan() {}

    private record Weighted(Neighbor neighbor, double weight) {}

    private record Group(List<Weighted> members, double score, Position centre) {}

    public static SocialAssessment assess(HerdMember self, List<Neighbor> neighbors, HerdingSettings settings) {
        List<Weighted> friendly = friendlyWithin(self, neighbors, settings, settings.scanRadius());
        double socialCount = 0.0;
        List<Position> herd = new ArrayList<>();
        for (Weighted w : friendly) {
            if (w.neighbor().position().distanceTo(self.position()) <= settings.herdRadius()) {
                socialCount += w.weight();
                herd.add(w.neighbor().position());
            }
        }

        double herdSize = 1.0 + socialCount;
        if (herdSize > settings.crowdFactor() * self.preference().value() && !herd.isEmpty()) {
            Position crowdCentre = Position.centroid(herd);
            return new SocialAssessment(
                    socialCount,
                    new HerdDecision.Disperse(self.position().awayFrom(crowdCentre, settings.herdRadius())));
        }

        if (socialCount >= self.preference().desiredMates() - settings.seekTolerance()) {
            return new SocialAssessment(socialCount, new HerdDecision.Content());
        }

        double felt = socialCount;
        return new SocialAssessment(felt, bestGroup(self, friendly, settings)
                .filter(group -> group.score() > felt + settings.seekTolerance())
                .<HerdDecision>map(group -> new HerdDecision.SeekHerd(group.centre()))
                .orElseGet(HerdDecision.Lonely::new));
    }

    /** Friendly neighbors within {@code radius} of the mob, with their social weights. */
    private static List<Weighted> friendlyWithin(
            HerdMember self, List<Neighbor> neighbors, HerdingSettings settings, double radius) {
        double radiusSquared = radius * radius;
        List<Weighted> result = new ArrayList<>();
        for (Neighbor n : neighbors) {
            if (n.position().distanceSquaredTo(self.position()) > radiusSquared) {
                continue;
            }
            double weight = SocialWeight.between(self, n, settings);
            if (weight > 0.0) {
                result.add(new Weighted(n, weight));
            }
        }
        return result;
    }

    /**
     * The group of friendly neighbors with the highest total weight. Each neighbor seeds a group of
     * everyone within the cluster radius of it; ties go to the closer group.
     */
    private static Optional<Group> bestGroup(HerdMember self, List<Weighted> friendly, HerdingSettings settings) {
        double clusterSquared = settings.clusterRadius() * settings.clusterRadius();
        Optional<Group> best = Optional.empty();
        for (Weighted seed : friendly) {
            List<Weighted> members = new ArrayList<>();
            List<Position> positions = new ArrayList<>();
            double score = 0.0;
            for (Weighted other : friendly) {
                if (other.neighbor().position().distanceSquaredTo(seed.neighbor().position()) <= clusterSquared) {
                    members.add(other);
                    positions.add(other.neighbor().position());
                    score += other.weight();
                }
            }
            Group candidate = new Group(members, score, Position.centroid(positions));
            if (best.isEmpty() || isBetter(self, candidate, best.get())) {
                best = Optional.of(candidate);
            }
        }
        return best;
    }

    private static boolean isBetter(HerdMember self, Group candidate, Group current) {
        double epsilon = 1.0e-9;
        if (candidate.score() > current.score() + epsilon) {
            return true;
        }
        if (candidate.score() < current.score() - epsilon) {
            return false;
        }
        return candidate.centre().distanceSquaredTo(self.position()) < current.centre().distanceSquaredTo(self.position());
    }
}
