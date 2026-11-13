package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import java.util.Map;
import java.util.Optional;

/**
 * Built-in diets (living-world spec, section 1 and the cow and sheep jobs). Cows prefer tall grass
 * and ferns, sheep prefer short grass. Horses and donkeys graze like cows but never crop the turf.
 */
public final class Diets {
    private Diets() {}

    private static final Diet COW = new Diet(Map.of(
                    Food.TALL_GRASS, Appetite.FAVOURITE,
                    Food.FERN, Appetite.FAVOURITE,
                    Food.SHORT_GRASS, Appetite.NORMAL),
            Diet.GrassCropping.WHEN_VERY_HUNGRY);
    private static final Diet SHEEP = new Diet(Map.of(
                    Food.SHORT_GRASS, Appetite.FAVOURITE,
                    Food.TALL_GRASS, Appetite.NORMAL,
                    Food.FLOWER, Appetite.NORMAL),
            Diet.GrassCropping.ALWAYS);
    private static final Diet HORSE = new Diet(Map.of(
                    Food.TALL_GRASS, Appetite.FAVOURITE,
                    Food.SHORT_GRASS, Appetite.NORMAL,
                    Food.FERN, Appetite.NORMAL),
            Diet.GrassCropping.NEVER);
    private static final Diet CHICKEN = new Diet(Map.of(
                    Food.SEED_ITEM, Appetite.FAVOURITE,
                    Food.SHORT_GRASS, Appetite.NORMAL),
            Diet.GrassCropping.NEVER);
    private static final Diet PIG = new Diet(Map.of(
                    Food.ROOT_ITEM, Appetite.FAVOURITE,
                    Food.ROOT_CROP, Appetite.NORMAL),
            Diet.GrassCropping.NEVER);
    private static final Diet RABBIT = new Diet(Map.of(
                    Food.FLOWER, Appetite.NORMAL,
                    Food.ROOT_CROP, Appetite.FAVOURITE,
                    Food.GRAIN_CROP, Appetite.NORMAL),
            Diet.GrassCropping.NEVER);

    private static final Map<SpeciesId, Diet> DIETS = Map.of(
            SpeciesId.minecraft("cow"), COW,
            SpeciesId.minecraft("mooshroom"), COW,
            SpeciesId.minecraft("sheep"), SHEEP,
            SpeciesId.minecraft("horse"), HORSE,
            SpeciesId.minecraft("donkey"), HORSE,
            SpeciesId.minecraft("chicken"), CHICKEN,
            SpeciesId.minecraft("pig"), PIG,
            SpeciesId.minecraft("rabbit"), RABBIT);

    public static Optional<Diet> forSpecies(SpeciesId species) {
        return Optional.ofNullable(DIETS.get(species));
    }
}
