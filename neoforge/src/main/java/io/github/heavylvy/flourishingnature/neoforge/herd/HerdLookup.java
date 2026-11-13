package io.github.heavylvy.flourishingnature.neoforge.herd;

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

    /** Herd-capable animals within {@code radius} of {@code animal}, not counting itself. */
    static List<Neighbor> neighbors(Animal animal, double radius) {
        List<Animal> nearby = animal.level().getEntitiesOfClass(
                Animal.class, animal.getBoundingBox().inflate(radius), other -> other.getId() != animal.getId() && other.isAlive());
        List<Neighbor> result = new ArrayList<>(nearby.size());
        for (Animal other : nearby) {
            preference(other).ifPresent(p -> result.add(
                    new Neighbor(EntityFacts.species(other), p, EntityFacts.position(other))));
        }
        return result;
    }
}
