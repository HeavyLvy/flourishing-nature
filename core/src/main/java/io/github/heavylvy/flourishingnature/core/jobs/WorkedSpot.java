package io.github.heavylvy.flourishingnature.core.jobs;

/** Where and when (game tick) an animal last did its job. */
public record WorkedSpot(BlockSpot spot, long tick) {}
