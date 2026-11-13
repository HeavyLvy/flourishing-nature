package io.github.heavylvy.flourishingnature.core.wellbeing;

/** What players can read from an animal's wellbeing. */
public enum Mood {
    /** 80 to 100: occasional happy particles, calmer sounds. */
    THRIVING,
    /** 50 to 79: nothing special. */
    CONTENT,
    /** 25 to 49: slower, more random wandering. */
    STRESSED,
    /** 0 to 24: grey particles, lowing. */
    MISERABLE;

    public static Mood of(Wellbeing wellbeing) {
        double v = wellbeing.value();
        if (v >= 80.0) {
            return THRIVING;
        }
        if (v >= 50.0) {
            return CONTENT;
        }
        if (v >= 25.0) {
            return STRESSED;
        }
        return MISERABLE;
    }
}
