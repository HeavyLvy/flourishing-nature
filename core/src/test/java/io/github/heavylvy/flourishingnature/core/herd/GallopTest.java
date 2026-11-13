package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class GallopTest {
    private static final Terrain OPEN = Optional::of;

    @Test
    void runsRoughlyStraightForItsWholeDistance() {
        SeededRandomness random = new SeededRandomness(5);
        Gallop gallop = Gallop.start(0.0, random);
        double planned = gallop.remaining();
        Position start = new Position(0, 64, 0);
        Position at = start;
        int strides = 0;
        Optional<Gallop.Stride> stride = gallop.step(at, OPEN, random);
        while (stride.isPresent()) {
            at = stride.get().anchor();
            stride = stride.get().next().step(at, OPEN, random);
            strides++;
        }
        assertTrue(planned >= Gallop.MIN_DISTANCE && planned <= Gallop.MAX_DISTANCE, "planned " + planned);
        double straightLine = at.distanceTo(start);
        assertTrue(straightLine > 0.85 * planned, "wandered too much: " + straightLine + " of " + planned);
        assertTrue(Math.abs(at.z()) > 0.5, "perfectly straight, no wobble at all");
        assertTrue(strides >= 7, "only " + strides + " strides");
    }

    @Test
    void swervesAroundALakeAhead() {
        Terrain lakeAhead = p -> p.x() > 5 && Math.abs(p.z()) < 3 ? Optional.empty() : Optional.of(p);
        Gallop.Stride stride = new Gallop(0.0, 200).step(new Position(0, 64, 0), lakeAhead, new SeededRandomness(6))
                .orElseThrow(() -> new AssertionError("gave up instead of swerving"));
        assertTrue(Math.abs(stride.anchor().z()) >= 3, "ran into the lake at " + stride.anchor());
    }

    @Test
    void stopsWhenWalledIn() {
        Terrain wall = p -> p.x() > 2 ? Optional.empty() : Optional.of(p);
        assertTrue(new Gallop(0.0, 200).step(new Position(0, 64, 0), wall, new SeededRandomness(7)).isEmpty());
    }
}
