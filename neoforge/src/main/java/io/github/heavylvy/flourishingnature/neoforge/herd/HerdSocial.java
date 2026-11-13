package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.HerdMember;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.core.herd.SocialScan;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.Optional;
import net.minecraft.world.entity.animal.Animal;

/** Herd facts other systems (wellbeing) can read. */
public final class HerdSocial {
    private HerdSocial() {}

    public static Optional<HerdPreference> preference(Animal animal) {
        return HerdLookup.preference(animal);
    }

    /** How many herd mates the animal feels it has right now, or empty if it doesn't herd. */
    public static Optional<Double> socialCount(Animal animal) {
        HerdingSettings settings = HerdingSettings.DEFAULT;
        return BuiltInProfiles.forSpecies(EntityFacts.species(animal)).flatMap(profile -> preference(animal)
                .map(p -> SocialScan.socialCount(
                        new HerdMember(profile, p, EntityFacts.position(animal)),
                        HerdLookup.neighbors(animal, settings.herdRadius()),
                        settings)));
    }
}
