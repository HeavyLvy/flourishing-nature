package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.Neighbor;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;

/** Reads herd data off animals in the world. */
final class HerdLookup {
    private HerdLookup() {}

    static Optional<HerdPreference> preference(Animal animal) {
        return animal.getExistingData(HerdAttachments.PREFERENCE);
    }

    static Optional<HerdId> herd(Animal animal) {
        return animal.getExistingData(HerdAttachments.HERD);
    }

    /** Herd-capable animals within {@code radius} of {@code animal}, not counting itself. */
    static List<Neighbor> neighbors(Animal animal, double radius) {
        List<Neighbor> result = new ArrayList<>();
        for (Animal other : herdAnimalsNear(animal, radius)) {
            preference(other).ifPresent(p -> result.add(
                    new Neighbor(EntityFacts.species(other), p, EntityFacts.position(other))));
        }
        return result;
    }

    /** Members of the same herd within {@code radius}, as neighbors. */
    static List<Neighbor> herdMates(Animal animal, double radius) {
        Optional<HerdId> mine = herd(animal);
        List<Neighbor> result = new ArrayList<>();
        for (Animal other : herdAnimalsNear(animal, radius)) {
            if (mine.isPresent() && herd(other).equals(mine)) {
                preference(other).ifPresent(p -> result.add(
                        new Neighbor(EntityFacts.species(other), p, EntityFacts.position(other))));
            }
        }
        return result;
    }

    /** Animals with a herd preference within {@code radius}, not counting {@code animal}. */
    static List<Animal> herdAnimalsNear(Animal animal, double radius) {
        return animal.level().getEntitiesOfClass(
                Animal.class,
                animal.getBoundingBox().inflate(radius),
                other -> other.getId() != animal.getId() && other.isAlive() && preference(other).isPresent());
    }
}
