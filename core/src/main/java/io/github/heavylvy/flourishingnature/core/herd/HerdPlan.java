package io.github.heavylvy.flourishingnature.core.herd;

import java.util.Map;

/**
 * The planner's answer.
 *
 * @param assignments the herd of every animal it was given, by handle
 * @param homes the home of every herd that still has members
 */
public record HerdPlan(Map<Integer, HerdId> assignments, Map<HerdId, HerdHome> homes) {
    public HerdPlan {
        assignments = Map.copyOf(assignments);
        homes = Map.copyOf(homes);
    }
}
