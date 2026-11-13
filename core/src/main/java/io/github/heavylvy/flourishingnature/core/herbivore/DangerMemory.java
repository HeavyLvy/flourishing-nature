package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The few places one animal remembers being attacked, newest last.
 *
 * @param spots remembered spots
 */
public record DangerMemory(List<DangerSpot> spots) {
    public static final DangerMemory EMPTY = new DangerMemory(List.of());

    public DangerMemory {
        spots = List.copyOf(spots);
    }

    /** Remembers an attack at {@code where}: refreshes a spot close by, forgets faded and excess spots. */
    public DangerMemory remember(Position where, long now, DangerSettings settings) {
        List<DangerSpot> kept = new ArrayList<>();
        for (DangerSpot spot : spots) {
            if (!spot.forgotten(now, settings) && spot.horizontalDistance(where) > settings.sameSpotWithin()) {
                kept.add(spot);
            }
        }
        kept.add(new DangerSpot(where, now));
        kept.sort(Comparator.comparingLong(DangerSpot::atTick));
        while (kept.size() > settings.maxSpots()) {
            kept.removeFirst();
        }
        return new DangerMemory(kept);
    }

    /** Spots still avoided now. */
    public List<DangerSpot> active(long now, DangerSettings settings) {
        return spots.stream().filter(s -> s.radius(now, settings) > 0.0).toList();
    }
}
