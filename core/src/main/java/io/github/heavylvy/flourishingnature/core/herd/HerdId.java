package io.github.heavylvy.flourishingnature.core.herd;

import java.util.Locale;
import java.util.UUID;

/** Names one herd. Every herd animal carries the id of the herd it belongs to. */
public record HerdId(UUID value) {
    /** First four hex digits, for debug tags. */
    public String shortName() {
        return value.toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    /** Stable per herd: about {@code chance} of all herds roam. */
    public boolean isRoamer(double chance) {
        double roll = Math.floorMod(value.getLeastSignificantBits() ^ value.getMostSignificantBits(), 10_000L) / 10_000.0;
        return roll < chance;
    }
}
