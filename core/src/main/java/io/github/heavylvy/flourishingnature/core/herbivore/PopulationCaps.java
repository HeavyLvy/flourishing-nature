package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Soft per-species population caps: past the cap within a small area, nobody self-breeds there.
 * Players can still breed animals by hand.
 *
 * @param defaultCap the cap for species without an override
 * @param overrides per-species caps
 */
public record PopulationCaps(int defaultCap, Map<SpeciesId, Integer> overrides) {
    public PopulationCaps {
        if (defaultCap < 1 || overrides.values().stream().anyMatch(cap -> cap < 1)) {
            throw new IllegalArgumentException("population caps must be at least 1");
        }
        overrides = Map.copyOf(overrides);
    }

    public int capFor(SpeciesId species) {
        return overrides.getOrDefault(species, defaultCap);
    }

    /**
     * Parses config lines like {@code minecraft:chicken=16}. Malformed lines are skipped and
     * returned in {@code rejected} so the loader can log them.
     */
    public static PopulationCaps parse(int defaultCap, List<String> lines, List<String> rejected) {
        Map<SpeciesId, Integer> caps = new HashMap<>();
        for (String line : lines) {
            int eq = line.lastIndexOf('=');
            try {
                SpeciesId species = new SpeciesId(line.substring(0, Math.max(0, eq)).trim());
                int cap = Integer.parseInt(line.substring(eq + 1).trim());
                if (cap < 1) {
                    rejected.add(line);
                } else {
                    caps.put(species, cap);
                }
            } catch (IllegalArgumentException e) {
                rejected.add(line);
            }
        }
        return new PopulationCaps(defaultCap, caps);
    }
}
