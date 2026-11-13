package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Who panics when a herd mate panics. Like a flock of starlings: one bird startles and the ones
 * around it take off too. Only friendly mobs within the alarm radius react, so a sheep's fright
 * doesn't send a pig running.
 */
public final class HerdAlarm {
    private HerdAlarm() {}

    /** Whether {@code listener} should panic because {@code panicking} did. */
    public static boolean spreadsTo(HerdMember panicking, Neighbor listener, HerdingSettings settings) {
        double radius = settings.alarmRadius();
        return listener.position().distanceSquaredTo(panicking.position()) <= radius * radius
                && SocialWeight.between(panicking, listener, settings) > 0.0;
    }
}
