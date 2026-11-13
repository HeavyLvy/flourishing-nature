package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Another herd-capable mob a mob can see.
 *
 * @param species its species
 * @param preference its own herd preference
 * @param position where it stands
 */
public record Neighbor(SpeciesId species, HerdPreference preference, Position position) {}
