package io.github.heavylvy.flourishingnature.core.herbivore;

/**
 * Self-breeding tuning.
 *
 * @param cooldownTicks ticks between self-breedings for one animal (3 in-game days)
 * @param checkInterval ticks between checks per animal
 * @param mateRadius a qualifying herd mate must be this close
 * @param capRadius the soft population cap counts animals of the species within this radius
 * @param defaultCap the cap when a species has no override
 */
public record SelfBreedingSettings(long cooldownTicks, int checkInterval, double mateRadius, double capRadius, int defaultCap) {

    public static final SelfBreedingSettings DEFAULT = new SelfBreedingSettings(72_000L, 200, 8.0, 16.0, 12);

    public SelfBreedingSettings {
        if (cooldownTicks < 0 || checkInterval < 1 || !(mateRadius > 0.0) || !(capRadius > 0.0) || defaultCap < 1) {
            throw new IllegalArgumentException("bad self-breeding settings");
        }
    }
}
