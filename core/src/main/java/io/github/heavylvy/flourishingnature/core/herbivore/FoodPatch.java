package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.Position;

/**
 * A spot near a herd's home, with how much food grows there.
 *
 * @param centre middle of the patch
 * @param food plants the herd eats there, tall grass counting double
 */
public record FoodPatch(Position centre, int food) {
    public FoodPatch {
        if (food < 0) {
            throw new IllegalArgumentException("food must be 0 or more: " + food);
        }
    }
}
