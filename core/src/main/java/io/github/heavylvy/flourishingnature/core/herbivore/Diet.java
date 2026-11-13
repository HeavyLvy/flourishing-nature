package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.fullness.FullnessRates;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What one species eats from the world, and how much it likes each food.
 *
 * @param foods every food it eats, with its appetite for it
 * @param cropsGrassBlocks whether it also crops grass blocks to dirt (sheep always, cows when very
 *     hungry)
 */
public record Diet(Map<Food, Appetite> foods, GrassCropping cropsGrassBlocks) {
    public Diet {
        if (foods.isEmpty()) {
            throw new IllegalArgumentException("a diet needs at least one food");
        }
        foods = Map.copyOf(new EnumMap<>(foods));
    }

    public boolean eats(Food food) {
        return foods.containsKey(food);
    }

    public Optional<Appetite> appetite(Food food) {
        return Optional.ofNullable(foods.get(food));
    }

    public Set<Food> menu() {
        return foods.keySet();
    }

    /** Fullness gained from one bite of {@code food}. Tall grass is two blocks of plant. */
    public static double meal(Food food) {
        return switch (food) {
            case TALL_GRASS -> FullnessRates.PLANT_MEAL * 1.5;
            case SHORT_GRASS, FERN, FLOWER, ROOT_CROP, GRAIN_CROP, SEED_ITEM, ROOT_ITEM -> FullnessRates.PLANT_MEAL;
        };
    }

    /** When an animal crops grass blocks under its feet to dirt. */
    public enum GrassCropping {
        /** Never. */
        NEVER,
        /** Now and then, like vanilla sheep, and more often when not full. */
        ALWAYS,
        /** Only when very hungry, when there is nothing better (cows). */
        WHEN_VERY_HUNGRY
    }
}
