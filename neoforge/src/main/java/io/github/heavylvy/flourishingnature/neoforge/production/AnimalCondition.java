package io.github.heavylvy.flourishingnature.neoforge.production;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import net.minecraft.world.entity.animal.Animal;

/** An animal's mood and hunger right now, the two inputs of {@link ProductionRules}. */
record AnimalCondition(Mood mood, HungerBand hunger) {

    /**
     * Only species the mod models (herd profiles) are affected; other animals never eat through
     * the fullness system, so reading their meter would make them all look starving.
     */
    static boolean tracked(Animal animal) {
        return BuiltInProfiles.forSpecies(EntityFacts.species(animal)).isPresent();
    }

    /** An animal never checked for wellbeing yet counts as content: vanilla behaviour. */
    static AnimalCondition of(Animal animal) {
        Mood mood = animal.getExistingData(WellbeingAttachments.STATE).map(s -> s.wellbeing().mood()).orElse(Mood.CONTENT);
        return new AnimalCondition(mood, FullnessApi.band(animal));
    }

    double productRate() {
        return ProductionRules.productRate(mood, hunger);
    }

    boolean canBreed() {
        return ProductionRules.canBreed(mood, hunger);
    }
}
