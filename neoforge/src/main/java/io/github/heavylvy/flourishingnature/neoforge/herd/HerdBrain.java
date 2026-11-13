package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdDecision;
import io.github.heavylvy.flourishingnature.core.herd.HerdMember;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.core.herd.Neighbor;
import io.github.heavylvy.flourishingnature.core.herd.SocialAssessment;
import io.github.heavylvy.flourishingnature.core.herd.SocialScan;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesProfile;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;

/**
 * One animal's view of its herd, refreshed every {@link #SCAN_INTERVAL} ticks and shared by both
 * herding goals so the neighbor scan only runs once. Not saved: it is rebuilt after loading.
 */
final class HerdBrain {
    static final int SCAN_INTERVAL = 40;

    private final Animal animal;
    private final SpeciesProfile profile;
    private final HerdingSettings settings;
    private long nextScan;
    private List<Neighbor> neighbors = List.of();
    private SocialAssessment assessment = new SocialAssessment(0.0, new HerdDecision.Content());

    HerdBrain(Animal animal, SpeciesProfile profile, HerdingSettings settings) {
        this.animal = animal;
        this.profile = profile;
        this.settings = settings;
        // Stagger scans by entity id so a field of cows doesn't all scan on the same tick.
        this.nextScan = animal.level().getGameTime() + Math.floorMod(animal.getId(), SCAN_INTERVAL);
    }

    Optional<HerdMember> self() {
        return HerdLookup.preference(animal).map(p -> new HerdMember(profile, p, EntityFacts.position(animal)));
    }

    /** Rescans if the interval has passed. Returns the latest assessment either way. */
    SocialAssessment refresh() {
        long now = animal.level().getGameTime();
        if (now >= nextScan) {
            nextScan = now + SCAN_INTERVAL;
            neighbors = HerdLookup.neighbors(animal, settings.scanRadius());
            assessment = self()
                    .map(me -> SocialScan.assess(me, neighbors, settings))
                    .orElse(assessment);
            HerdDebug.show(animal, preferenceOrLoner(), assessment);
        }
        return assessment;
    }

    List<Neighbor> neighbors() {
        return neighbors;
    }

    HerdingSettings settings() {
        return settings;
    }

    private HerdPreference preferenceOrLoner() {
        return HerdLookup.preference(animal).orElse(new HerdPreference(HerdPreference.MIN));
    }

    /** Herding is off while a player is controlling the animal or it is too young. */
    boolean canHerd() {
        return animal.isAlive()
                && !animal.isBaby()
                && !animal.isPassenger()
                && !animal.isVehicle()
                && !EntityFacts.isLeashed(animal);
    }
}
