package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdMember;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesProfile;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

/** What the herding goals need to know about one animal and its herd. Not saved. */
final class HerdBrain {
    private final Animal animal;
    private final SpeciesProfile profile;
    private final HerdingSettings settings;

    HerdBrain(Animal animal, SpeciesProfile profile, HerdingSettings settings) {
        this.animal = animal;
        this.profile = profile;
        this.settings = settings;
    }

    Optional<HerdMember> self() {
        return HerdLookup.preference(animal).map(p -> new HerdMember(profile, p, EntityFacts.position(animal)));
    }

    HerdingSettings settings() {
        return settings;
    }

    Optional<HerdId> herd() {
        return HerdLookup.herd(animal);
    }

    Optional<HerdHome> home() {
        return animal.level() instanceof ServerLevel level
                ? herd().flatMap(id -> HerdManager.of(level).home(id))
                : Optional.empty();
    }

    boolean isGalloping() {
        return animal.level() instanceof ServerLevel level
                && herd().map(id -> HerdManager.of(level).isGalloping(id)).orElse(false);
    }

    int herdSize() {
        return animal.level() instanceof ServerLevel level
                ? herd().map(id -> HerdManager.of(level).size(id)).orElse(0)
                : 0;
    }

    /** Pets don't join herds; babies do (they follow a parent until grown). */
    static boolean joinsHerds(Animal animal) {
        return !(animal instanceof TamableAnimal pet && pet.isTame());
    }

    /** Whether the herding goals should steer this animal right now. */
    boolean canHerd() {
        return animal.isAlive()
                && joinsHerds(animal)
                && !animal.isBaby()
                && !animal.isPassenger()
                && !animal.isVehicle()
                && !EntityFacts.isLeashed(animal)
                && !isAlarmed();
    }

    /** While a herd mate's panic is active, herding is off so the herd scatters. */
    private boolean isAlarmed() {
        long now = animal.level().getGameTime();
        return animal.getExistingData(HerdAttachments.ALARM).filter(alarm -> alarm.isActive(now)).isPresent();
    }
}
