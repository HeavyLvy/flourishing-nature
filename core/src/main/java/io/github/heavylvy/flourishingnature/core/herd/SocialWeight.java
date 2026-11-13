package io.github.heavylvy.flourishingnature.core.herd;

/**
 * How much a neighbor counts as a herd mate:
 *
 * <pre>w(a, b) = affinity(a, b) * (1 - |pref_a - pref_b| / maxPref) ^ k</pre>
 *
 * <p>So like-minded mobs cluster: a cow that wants 8 mates cares less about one that wants 2.
 * Hostile (negative) affinities are returned unscaled and never count toward a herd.
 */
public final class SocialWeight {
    private SocialWeight() {}

    public static double between(HerdMember self, Neighbor other, HerdingSettings settings) {
        Affinity affinity = self.profile().affinityTo(other.species());
        if (!affinity.isFriendly()) {
            return affinity.value();
        }
        double difference = Math.abs(self.preference().value() - other.preference().value());
        double similarity = 1.0 - difference / HerdPreference.MAX;
        return affinity.value() * Math.pow(similarity, settings.similarityExponent());
    }
}
