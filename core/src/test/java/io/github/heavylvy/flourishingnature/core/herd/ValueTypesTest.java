package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ValueTypesTest {
    @Test
    void herdPreferenceRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new HerdPreference(0));
        assertThrows(IllegalArgumentException.class, () -> new HerdPreference(HerdPreference.MAX + 1));
        assertEquals(HerdPreference.MAX, HerdPreference.clamped(99).value());
    }

    @Test
    void speciesIdMustBeNamespaced() {
        assertThrows(IllegalArgumentException.class, () -> new SpeciesId("cow"));
        assertThrows(IllegalArgumentException.class, () -> new SpeciesId("Minecraft:Cow"));
    }

    @Test
    void affinityIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> new Affinity(1.5));
        assertThrows(IllegalArgumentException.class, () -> new Affinity(Double.NaN));
    }

    @Test
    void profileCannotListItself() {
        PreferenceDistribution d = new PreferenceDistribution(5, IntRange.of(3, 8), 0, List.of());
        assertThrows(IllegalArgumentException.class,
                () -> new SpeciesProfile(BuiltInProfiles.COW, d, Map.of(BuiltInProfiles.COW, new Affinity(0.5))));
    }

    @Test
    void everyBuiltInProfileLoads() {
        assertEquals(8, BuiltInProfiles.all().size());
    }
}
