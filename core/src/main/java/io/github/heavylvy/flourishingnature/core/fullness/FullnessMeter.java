package io.github.heavylvy.flourishingnature.core.fullness;

/**
 * A fullness reading plus when it was taken. Drains lazily: the current value is worked out from
 * the elapsed game time whenever someone asks, so nothing needs to tick.
 *
 * @param fullness fullness at {@code atTick}
 * @param atTick game tick of the reading
 */
public record FullnessMeter(Fullness fullness, long atTick) {

    /** A fresh animal starts comfortably fed but not full, so it must eat before breeding. */
    public static FullnessMeter newborn(long now) {
        return new FullnessMeter(new Fullness(0.6), now);
    }

    /** Fullness now, after draining at {@code drainPerTick} since the reading. */
    public Fullness now(long now, double drainPerTick) {
        long elapsed = Math.max(0L, now - atTick);
        return Fullness.clamped(fullness.value() - elapsed * drainPerTick);
    }

    /** A new reading after eating {@code amount} (0 to 1) now. */
    public FullnessMeter eat(double amount, long now, double drainPerTick) {
        return new FullnessMeter(now(now, drainPerTick).plus(amount), now);
    }

    /** A new reading set to {@code value} now, for a wolf pack's kill or a debug command. */
    public static FullnessMeter at(Fullness value, long now) {
        return new FullnessMeter(value, now);
    }
}
