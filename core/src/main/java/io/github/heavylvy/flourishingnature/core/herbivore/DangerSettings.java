package io.github.heavylvy.flourishingnature.core.herbivore;

/**
 * How herds remember attacks. Times in ticks, distances in blocks.
 *
 * @param radius herd homes stay at least this far from a fresh danger spot
 * @param holdTicks the spot is avoided at full radius this long (1 day)
 * @param fadeTicks then the avoided area shrinks to nothing over this long (1 more day)
 * @param maxSpots most spots one animal remembers; the oldest is forgotten first
 * @param sameSpotWithin a new attack this close to a remembered one refreshes it
 * @param shareRadius herd mates this close to the attack remember it too
 */
public record DangerSettings(
        double radius, long holdTicks, long fadeTicks, int maxSpots, double sameSpotWithin, double shareRadius) {

    public static final DangerSettings DEFAULT = new DangerSettings(16.0, 24_000L, 24_000L, 4, 4.0, 16.0);

    public DangerSettings {
        if (!(radius > 0.0) || holdTicks < 0 || fadeTicks < 1 || maxSpots < 1 || !(sameSpotWithin >= 0.0)
                || !(shareRadius >= 0.0)) {
            throw new IllegalArgumentException("bad danger settings");
        }
    }
}
