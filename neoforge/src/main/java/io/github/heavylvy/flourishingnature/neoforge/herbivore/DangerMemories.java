package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.herbivore.DangerMemory;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerSettings;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.FleeTargets;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Herd animals remember where they were attacked, and so do herd mates who saw it. */
public final class DangerMemories {
    private DangerMemories() {}

    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !HerbivoreConfig.dangerMemory()
                || !(event.getEntity() instanceof Animal victim)
                || FleeTargets.attacker(event.getSource()).filter(a -> a.getId() != victim.getId()).isEmpty()) {
            return;
        }
        attacked(victim);
    }

    /** {@code victim} was attacked where it stands: it and its herd mates nearby remember the spot. */
    public static void attacked(Animal victim) {
        Optional<HerdId> herd = victim.getExistingData(HerdAttachments.HERD);
        if (herd.isEmpty()) {
            return;
        }
        DangerSettings settings = HerbivoreConfig.dangerSettings();
        Position where = EntityFacts.position(victim);
        long now = victim.level().getGameTime();
        remember(victim, where, now);
        for (Animal mate : victim.level().getEntitiesOfClass(Animal.class, victim.getBoundingBox().inflate(settings.shareRadius()),
                other -> other.getId() != victim.getId() && other.isAlive() && other.getExistingData(HerdAttachments.HERD).equals(herd))) {
            remember(mate, where, now);
        }
    }

    /** Adds a danger spot to one animal's memory. Public so tests can plant one. */
    public static void remember(Animal animal, Position where, long tick) {
        DangerMemory memory = animal.getExistingData(HerbivoreAttachments.DANGER).orElse(DangerMemory.EMPTY);
        animal.setData(HerbivoreAttachments.DANGER, memory.remember(where, tick, HerbivoreConfig.dangerSettings()));
    }

    public static DangerMemory of(Animal animal) {
        return animal.getExistingData(HerbivoreAttachments.DANGER).orElse(DangerMemory.EMPTY);
    }
}
