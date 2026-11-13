package io.github.heavylvy.flourishingnature.core.jobs;

/**
 * A sheep's fleece holds one seed at a time. Only unsheared sheep catch or keep one, and a seed
 * only takes on a free grass block, never bare dirt.
 */
public final class SeedRules {
    private SeedRules() {}

    public static boolean mayCatch(boolean sheared, boolean carrying) {
        return !sheared && !carrying;
    }

    /** Carried long enough to have left the plant it came from. */
    public static boolean readyToDrop(long caughtAt, long now, long minCarryTicks) {
        return now - caughtAt >= minCarryTicks;
    }

    public static boolean takesRoot(boolean grassBelow, boolean spaceFree) {
        return grassBelow && spaceFree;
    }
}
