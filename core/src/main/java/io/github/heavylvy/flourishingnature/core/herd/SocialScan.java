package io.github.heavylvy.flourishingnature.core.herd;

import java.util.List;

/** How many herd mates a mob feels it has: the weighted count of friendly neighbors close by. */
public final class SocialScan {
    private SocialScan() {}

    public static double socialCount(HerdMember self, List<Neighbor> neighbors, HerdingSettings settings) {
        double radiusSquared = settings.herdRadius() * settings.herdRadius();
        double count = 0.0;
        for (Neighbor n : neighbors) {
            if (n.position().distanceSquaredTo(self.position()) <= radiusSquared) {
                count += Math.max(0.0, SocialWeight.between(self, n, settings));
            }
        }
        return count;
    }
}
