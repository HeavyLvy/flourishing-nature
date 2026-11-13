package io.github.heavylvy.flourishingnature.core.herd;

/**
 * A loader-neutral entity type id such as {@code minecraft:cow}.
 *
 * @param value a namespaced id, {@code namespace:path}
 */
public record SpeciesId(String value) {
    public SpeciesId {
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("not a namespaced id: '" + value + "'");
        }
    }

    public static SpeciesId minecraft(String path) {
        return new SpeciesId("minecraft:" + path);
    }
}
