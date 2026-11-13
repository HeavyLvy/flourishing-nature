package io.github.heavylvy.flourishingnature.core.herd;

/** Whether an animal belongs to a herd yet. */
public sealed interface Membership {
    record Unassigned() implements Membership {}

    record InHerd(HerdId herd) implements Membership {}
}
