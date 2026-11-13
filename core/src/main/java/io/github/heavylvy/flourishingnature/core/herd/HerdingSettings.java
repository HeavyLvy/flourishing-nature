package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Tuning numbers for how individual animals sense and space themselves. Distances are in blocks.
 * Herd-level choices (join, merge, split, roam) are in {@link HerdPlanSettings}.
 *
 * @param scanRadius how far an animal looks for neighbors
 * @param herdRadius neighbors closer than this count toward how many mates it feels it has
 * @param similarityExponent how strongly mobs prefer mates with a similar preference (k)
 * @param minSpacing cohesion pushes a mob away from a mate closer than this
 * @param maxSpacing cohesion pulls a mob back when its nearest mate is farther than this
 * @param alarmRadius herd mates within this distance of a panicking mob panic too
 */
public record HerdingSettings(
        double scanRadius,
        double herdRadius,
        double similarityExponent,
        double minSpacing,
        double maxSpacing,
        double alarmRadius) {

    public static final HerdingSettings DEFAULT = new HerdingSettings(16.0, 8.0, 1.5, 2.0, 4.0, 10.0);

    public HerdingSettings {
        requirePositive("scanRadius", scanRadius);
        requirePositive("herdRadius", herdRadius);
        requirePositive("similarityExponent", similarityExponent);
        requirePositive("minSpacing", minSpacing);
        requirePositive("alarmRadius", alarmRadius);
        if (herdRadius > scanRadius) {
            throw new IllegalArgumentException("herdRadius must not exceed scanRadius");
        }
        if (!(maxSpacing > minSpacing)) {
            throw new IllegalArgumentException("maxSpacing must be above minSpacing");
        }
    }

    private static void requirePositive(String name, double value) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a positive number: " + value);
        }
    }
}
