package io.github.heavylvy.flourishingnature.core.herd;

/**
 * A herd mate panicked nearby. While active, the animal flees from {@code source} and ignores herd
 * cohesion, so the herd scatters instead of running as one block.
 *
 * @param source where the danger is (the attacker or predator, or the hurt mate if unknown)
 * @param expiresAt game tick when the alarm ends
 * @param chaserSpeed how fast the danger chases, in blocks per tick, or infinity when it isn't an
 *     animal hunter (a player, an explosion), in which case the herd runs at full panic speed
 */
public record PanicAlarm(Position source, long expiresAt, double chaserSpeed) {
    public PanicAlarm {
        if (!(chaserSpeed > 0.0)) {
            throw new IllegalArgumentException("chaser speed must be above 0: " + chaserSpeed);
        }
    }

    public static PanicAlarm unknownChaser(Position source, long expiresAt) {
        return new PanicAlarm(source, expiresAt, Double.POSITIVE_INFINITY);
    }

    public boolean isActive(long now) {
        return now < expiresAt;
    }
}
