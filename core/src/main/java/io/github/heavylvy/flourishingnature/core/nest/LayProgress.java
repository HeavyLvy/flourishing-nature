package io.github.heavylvy.flourishingnature.core.nest;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;

/**
 * How close a hen is to her next egg, in "content hen" ticks. A content, fed hen gains one per
 * tick and lays every {@link NestSettings#layIntervalTicks()}; a thriving one twice as fast; a
 * starving or miserable one never (see {@link ProductionRules#productRate}).
 *
 * @param ticks progress toward the next egg, never negative
 */
public record LayProgress(double ticks) {
    public static final LayProgress NONE = new LayProgress(0);

    public LayProgress {
        if (!(ticks >= 0) || Double.isInfinite(ticks)) {
            throw new IllegalArgumentException("lay progress must be finite and >= 0: " + ticks);
        }
    }

    /**
     * {@code elapsed} ticks passed at this mood and hunger. Progress stops at a full egg's worth:
     * a hen that cannot lay (no nest, nest full) does not bank eggs.
     */
    public LayProgress advance(long elapsed, Mood mood, HungerBand hunger, NestSettings settings) {
        double rate = ProductionRules.productRate(mood, hunger);
        return new LayProgress(Math.min((double) settings.layIntervalTicks(), ticks + (double) elapsed * rate));
    }

    public boolean ready(NestSettings settings) {
        return ticks >= (double) settings.layIntervalTicks();
    }
}
