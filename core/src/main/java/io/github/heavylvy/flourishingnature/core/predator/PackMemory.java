package io.github.heavylvy.flourishingnature.core.predator;

/**
 * What one wolf remembers about its pack life. Kept on each wolf, never per chunk.
 *
 * @param lastMeal game tick of its last kill, or of a fresh start (first seen, or after its pack
 *     broke up), so a new wolf is not counted as days without food
 * @param lastBred game tick it last had a pup; very negative if never
 */
public record PackMemory(long lastMeal, long lastBred) {
    public static final long NEVER = Long.MIN_VALUE / 4;

    public static PackMemory fresh(long now) {
        return new PackMemory(now, NEVER);
    }

    public PackMemory fed(long now) {
        return new PackMemory(now, lastBred);
    }

    public PackMemory bred(long now) {
        return new PackMemory(lastMeal, now);
    }
}
