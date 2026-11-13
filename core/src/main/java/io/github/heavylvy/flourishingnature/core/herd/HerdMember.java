package io.github.heavylvy.flourishingnature.core.herd;

/**
 * The mob doing the looking.
 *
 * @param profile its species profile
 * @param preference its herd preference
 * @param position where it stands
 */
public record HerdMember(SpeciesProfile profile, HerdPreference preference, Position position) {}
