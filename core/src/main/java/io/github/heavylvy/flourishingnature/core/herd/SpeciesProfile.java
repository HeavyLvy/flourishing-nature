package io.github.heavylvy.flourishingnature.core.herd;

import java.util.Map;

/**
 * Everything the herding rules need to know about one species.
 *
 * @param species the species this profile describes
 * @param preference how it rolls its herd preference
 * @param affinities how much it likes other species; its own species is always {@link
 *     Affinity#SAME_SPECIES} and unlisted species are {@link Affinity#NONE}
 */
public record SpeciesProfile(SpeciesId species, PreferenceDistribution preference, Map<SpeciesId, Affinity> affinities) {

    public SpeciesProfile {
        if (affinities.containsKey(species)) {
            throw new IllegalArgumentException(species.value() + " lists an affinity to itself; it is always 1.0");
        }
        affinities = Map.copyOf(affinities);
    }

    public Affinity affinityTo(SpeciesId other) {
        if (other.equals(species)) {
            return Affinity.SAME_SPECIES;
        }
        return affinities.getOrDefault(other, Affinity.NONE);
    }
}
