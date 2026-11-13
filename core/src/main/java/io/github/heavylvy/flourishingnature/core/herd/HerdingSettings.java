package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Tuning numbers for herding. Distances are in blocks.
 *
 * @param scanRadius how far a mob looks for herds to join
 * @param herdRadius neighbors closer than this count toward its herd
 * @param clusterRadius neighbors this close to each other are treated as one group when choosing
 *     where to go
 * @param crowdFactor a herd larger than {@code preference * crowdFactor} makes the mob drift away
 * @param seekTolerance how many mates short of its wish a mob can be before it goes looking
 * @param similarityExponent how strongly mobs prefer mates with a similar preference (k)
 * @param minSpacing cohesion pushes a mob away from a mate closer than this
 * @param maxSpacing cohesion pulls a mob back when its nearest mate is farther than this
 * @param alarmRadius herd mates within this distance of a panicking mob panic too
 */
public record HerdingSettings(
        double scanRadius,
        double herdRadius,
        double clusterRadius,
        double crowdFactor,
        double seekTolerance,
        double similarityExponent,
        double minSpacing,
        double maxSpacing,
        double alarmRadius) {

    public static final HerdingSettings DEFAULT = new HerdingSettings(16.0, 8.0, 6.0, 1.5, 0.5, 1.5, 2.0, 4.0, 10.0);

    public HerdingSettings {
        requirePositive("scanRadius", scanRadius);
        requirePositive("herdRadius", herdRadius);
        requirePositive("clusterRadius", clusterRadius);
        requirePositive("similarityExponent", similarityExponent);
        requirePositive("minSpacing", minSpacing);
        requirePositive("alarmRadius", alarmRadius);
        if (!(crowdFactor > 1.0)) {
            throw new IllegalArgumentException("crowdFactor must be above 1: " + crowdFactor);
        }
        if (!(seekTolerance >= 0.0)) {
            throw new IllegalArgumentException("seekTolerance must be 0 or more: " + seekTolerance);
        }
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
