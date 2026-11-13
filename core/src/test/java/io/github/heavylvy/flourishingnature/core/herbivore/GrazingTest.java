package io.github.heavylvy.flourishingnature.core.herbivore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class GrazingTest {
    private static final GrazingSettings SETTINGS = GrazingSettings.DEFAULT;

    private record Option(ForageCandidate candidate) implements Grazing.HasCandidate {}

    private static Diet diet(String species) {
        return Diets.forSpecies(SpeciesId.minecraft(species)).orElseThrow();
    }

    private static Optional<Food> pick(Diet diet, ForageCandidate... candidates) {
        return Grazing.best(diet, List.of(candidates).stream().map(Option::new).toList())
                .map(o -> o.candidate().food());
    }

    @Test
    void cowsWalkFurtherForTallGrassAndFerns() {
        Diet cow = diet("cow");
        assertEquals(Optional.of(Food.TALL_GRASS),
                pick(cow, new ForageCandidate(Food.SHORT_GRASS, 3), new ForageCandidate(Food.TALL_GRASS, 5)));
        assertEquals(Optional.of(Food.FERN),
                pick(cow, new ForageCandidate(Food.SHORT_GRASS, 2), new ForageCandidate(Food.FERN, 4)));
        assertEquals(Optional.of(Food.SHORT_GRASS),
                pick(cow, new ForageCandidate(Food.SHORT_GRASS, 1), new ForageCandidate(Food.TALL_GRASS, 8)),
                "but not across the whole field");
    }

    @Test
    void sheepPreferShortGrass() {
        assertEquals(Optional.of(Food.SHORT_GRASS),
                pick(diet("sheep"), new ForageCandidate(Food.TALL_GRASS, 3), new ForageCandidate(Food.SHORT_GRASS, 5)));
    }

    @Test
    void eachSpeciesEatsOnlyItsOwnFood() {
        assertEquals(Optional.empty(), pick(diet("pig"), new ForageCandidate(Food.SHORT_GRASS, 1)));
        assertEquals(Optional.of(Food.ROOT_ITEM), pick(diet("pig"), new ForageCandidate(Food.ROOT_ITEM, 6)));
        assertEquals(Optional.of(Food.SEED_ITEM),
                pick(diet("chicken"), new ForageCandidate(Food.FLOWER, 1), new ForageCandidate(Food.SEED_ITEM, 3)));
        assertEquals(Optional.of(Food.FLOWER), pick(diet("rabbit"), new ForageCandidate(Food.FLOWER, 2)));
        assertTrue(Diets.forSpecies(SpeciesId.minecraft("wolf")).isEmpty());
    }

    @Test
    void onlyAnimalsThatAreNotFullGraze() {
        assertTrue(Grazing.wantsToEat(new Fullness(0.5), SETTINGS));
        assertFalse(Grazing.wantsToEat(Fullness.FULL, SETTINGS));
    }

    @Test
    void cowsCropTurfOnlyWhenVeryHungrySheepAlways() {
        assertFalse(Grazing.cropsGrass(diet("cow"), new Fullness(0.5), SETTINGS));
        assertTrue(Grazing.cropsGrass(diet("cow"), new Fullness(0.1), SETTINGS));
        assertTrue(Grazing.cropsGrass(diet("sheep"), Fullness.FULL, SETTINGS));
        assertFalse(Grazing.cropsGrass(diet("horse"), Fullness.EMPTY, SETTINGS));
    }

    @Test
    void tallGrassIsABiggerMeal() {
        assertTrue(Diet.meal(Food.TALL_GRASS) > Diet.meal(Food.SHORT_GRASS));
        assertTrue(Food.SEED_ITEM.isItem());
        assertFalse(Food.FERN.isItem());
    }

    @Test
    void settingsRejectNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new GrazingSettings(0.0, 0.0, 8, 100, 8));
        assertThrows(IllegalArgumentException.class, () -> new GrazingSettings(0.5, 0.6, 8, 100, 8));
        assertThrows(IllegalArgumentException.class, () -> new ForageCandidate(Food.FERN, -1));
        assertThrows(IllegalArgumentException.class, () -> new Appetite(0));
    }
}
