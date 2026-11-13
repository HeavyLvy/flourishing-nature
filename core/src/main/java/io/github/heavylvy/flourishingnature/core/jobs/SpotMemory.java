package io.github.heavylvy.flourishingnature.core.jobs;

import java.util.ArrayList;
import java.util.List;

/**
 * The few spots an animal worked recently, so it leaves each one alone for a cooldown (pigs:
 * several days per spot). Oldest spots are forgotten first once the memory is full.
 */
public record SpotMemory(List<WorkedSpot> spots) {
    public static final SpotMemory EMPTY = new SpotMemory(List.of());

    public SpotMemory {
        spots = List.copyOf(spots);
    }

    /** No remembered spot within {@code radius} is younger than {@code cooldownTicks}. */
    public boolean mayWorkAt(BlockSpot spot, long now, long cooldownTicks, int radius) {
        return spots.stream().noneMatch(w -> w.spot().within(spot, radius) && now - w.tick() < cooldownTicks);
    }

    /** Remembers {@code spot}, dropping expired spots and then the oldest past {@code capacity}. */
    public SpotMemory remember(BlockSpot spot, long now, long cooldownTicks, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1: " + capacity);
        }
        List<WorkedSpot> kept = new ArrayList<>();
        for (WorkedSpot w : spots) {
            if (now - w.tick() < cooldownTicks) {
                kept.add(w);
            }
        }
        kept.add(new WorkedSpot(spot, now));
        while (kept.size() > capacity) {
            kept.removeFirst();
        }
        return new SpotMemory(kept);
    }
}
