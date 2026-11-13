package io.github.heavylvy.flourishingnature.core.herd;

/**
 * One loaded herd animal, as the herd planner sees it.
 *
 * @param handle the loader's id for this animal, used to hand back its new herd
 * @param profile its species profile
 * @param preference its herd preference
 * @param position where it stands
 * @param membership its current herd, if any
 */
public record HerdAnimal(int handle, SpeciesProfile profile, HerdPreference preference, Position position, Membership membership) {
    public SpeciesId species() {
        return profile.species();
    }
}
