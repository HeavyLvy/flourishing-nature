package io.github.heavylvy.flourishingnature.core.jobs;

/**
 * Turns "about N times a day" into a chance per check. One in-game day is 24,000 ticks.
 */
public final class JobChance {
    private JobChance() {}

    public static final long DAY = 24_000L;

    /** Blocks in a chunk section; each random tick picks one of them. */
    private static final double SECTION_BLOCKS = 4096.0;

    /**
     * Chance per check, run every {@code intervalTicks}, so the event happens about
     * {@code perDay} times a day. Capped at 1.
     */
    public static double perCheck(double perDay, long intervalTicks) {
        if (perDay < 0 || intervalTicks < 1) {
            throw new IllegalArgumentException("perDay >= 0 and interval >= 1: " + perDay + ", " + intervalTicks);
        }
        return Math.min(1.0, perDay * intervalTicks / DAY);
    }

    /**
     * Chance per random tick so a block steps about once every {@code days} in-game days. A block
     * gets on average {@code randomTickSpeed / 4096} random ticks per game tick (vanilla default
     * speed is 3, about 17.6 random ticks a day). Speed 0 means random ticks are off.
     */
    public static double perRandomTick(double days, int randomTickSpeed) {
        if (days <= 0 || randomTickSpeed < 0) {
            throw new IllegalArgumentException("days > 0 and speed >= 0: " + days + ", " + randomTickSpeed);
        }
        if (randomTickSpeed == 0) {
            return 0.0;
        }
        double ticksPerDay = randomTickSpeed / SECTION_BLOCKS * DAY;
        return Math.min(1.0, 1.0 / (days * ticksPerDay));
    }
}
