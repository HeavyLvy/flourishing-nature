package io.github.heavylvy.flourishingnature.core.herd;

import java.util.Optional;

/** What the planner may ask about the world. Implemented by each loader. */
public interface Terrain {
    /**
     * A good grazing spot at or near {@code near}: solid dry ground (not water) with open sky
     * above, snapped to the surface. Empty if there is none there, such as in a lake or a cave.
     */
    Optional<Position> pasture(Position near);
}
