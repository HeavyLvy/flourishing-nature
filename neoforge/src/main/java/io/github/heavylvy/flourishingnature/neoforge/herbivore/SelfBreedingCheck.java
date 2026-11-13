package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.herbivore.SelfBreeding;
import io.github.heavylvy.flourishingnature.core.herbivore.SelfBreedingSettings;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Love;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Herd animals that are full and happy, next to a herd mate in the same state, fall in love on
 * their own; vanilla's breeding goal does the rest. Each then waits a few in-game days.
 */
public final class SelfBreedingCheck {
    private SelfBreedingCheck() {}

    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide()) {
            return;
        }
        long now = animal.level().getGameTime();
        if (Math.floorMod(now + animal.getId() * 7L, HerbivoreConfig.selfBreedingSettings().checkInterval()) == 0) {
            checkNow(animal);
        }
    }

    /**
     * Runs one self-breeding check now. Public so gametests can force one.
     *
     * @return what the animal decided
     */
    public static SelfBreeding.Verdict checkNow(Animal animal) {
        if (!HerbivoreConfig.selfBreeding()) {
            return SelfBreeding.Verdict.NOT_READY_TO_LOVE;
        }
        Optional<HerdId> herd = animal.getExistingData(HerdAttachments.HERD);
        if (herd.isEmpty()) {
            return SelfBreeding.Verdict.NOT_READY_TO_LOVE;
        }
        SelfBreeding.Verdict mine = verdict(animal);
        if (mine != SelfBreeding.Verdict.READY) {
            return mine;
        }
        SelfBreedingSettings settings = HerbivoreConfig.selfBreedingSettings();
        SpeciesId species = EntityFacts.species(animal);
        Optional<Animal> mate = animal.level().getEntitiesOfClass(Animal.class, animal.getBoundingBox().inflate(settings.mateRadius()),
                        other -> other.getId() != animal.getId() && other.isAlive() && EntityFacts.species(other).equals(species)
                                && other.getExistingData(HerdAttachments.HERD).equals(herd))
                .stream()
                .filter(other -> verdict(other) == SelfBreeding.Verdict.READY)
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(animal)));
        mate.ifPresent(other -> {
            long now = animal.level().getGameTime();
            for (Animal parent : new Animal[] {animal, other}) {
                Love.startWithoutPlayer(parent);
                parent.setData(HerbivoreAttachments.LAST_SELF_BRED, now);
            }
        });
        return mate.isPresent() ? SelfBreeding.Verdict.READY : SelfBreeding.Verdict.NOT_READY_TO_LOVE;
    }

    /** One animal's own verdict, mate aside. */
    public static SelfBreeding.Verdict verdict(Animal animal) {
        Optional<Mood> mood = animal.getExistingData(WellbeingAttachments.STATE).map(s -> s.wellbeing().mood());
        if (mood.isEmpty()) {
            return SelfBreeding.Verdict.NOT_WELL_ENOUGH;
        }
        SelfBreedingSettings settings = HerbivoreConfig.selfBreedingSettings();
        SpeciesId species = EntityFacts.species(animal);
        int nearby = 1 + animal.level().getEntitiesOfClass(Animal.class, animal.getBoundingBox().inflate(settings.capRadius()),
                other -> other.getId() != animal.getId() && other.isAlive() && EntityFacts.species(other).equals(species)).size();
        long last = animal.getExistingData(HerbivoreAttachments.LAST_SELF_BRED).orElse(Long.MIN_VALUE / 2);
        return SelfBreeding.check(animal.canFallInLove(), mood.get(), FullnessApi.band(animal), animal.level().getGameTime(), last,
                nearby, HerbivoreConfig.populationCaps().capFor(species), settings);
    }
}
