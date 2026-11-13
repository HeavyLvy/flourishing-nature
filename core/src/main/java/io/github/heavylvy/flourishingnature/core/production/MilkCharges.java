package io.github.heavylvy.flourishingnature.core.production;

import java.util.Optional;

/**
 * Milk an animal has ready, plus how far the next charge has refilled, as of a game tick. Refills
 * lazily: the current value is worked out from elapsed time whenever someone asks, at the product
 * rate the animal has right now.
 *
 * @param charges ready charges, 0 or more
 * @param progress refill progress toward the next charge, in [0, 1); 0 when full
 * @param atTick game tick of this reading
 */
public record MilkCharges(int charges, double progress, long atTick) {
    public MilkCharges {
        if (charges < 0) {
            throw new IllegalArgumentException("charges must be 0 or more: " + charges);
        }
        if (!(progress >= 0.0 && progress < 1.0)) {
            throw new IllegalArgumentException("progress must be in [0, 1): " + progress);
        }
    }

    /** A new animal starts with a full udder. */
    public static MilkCharges full(MilkSettings settings, long now) {
        return new MilkCharges(settings.maxCharges(), 0.0, now);
    }

    /**
     * The reading at {@code now}, refilling at {@code rate} (see {@link ProductionRules#productRate})
     * since {@link #atTick}.
     */
    public MilkCharges now(long now, double rate, MilkSettings settings) {
        if (!(rate >= 0.0) || Double.isInfinite(rate)) {
            throw new IllegalArgumentException("rate must be finite and 0 or more: " + rate);
        }
        int max = settings.maxCharges();
        if (charges >= max) {
            return new MilkCharges(max, 0.0, now);
        }
        long elapsed = Math.max(0L, now - atTick);
        double refilled = progress + elapsed * rate / settings.ticksPerCharge();
        int gained = (int) Math.min(max - charges, Math.floor(refilled));
        int total = charges + gained;
        double left = total >= max ? 0.0 : refilled - gained;
        return new MilkCharges(total, left, now);
    }

    /** Takes one charge at {@code now}; empty when none is ready (the animal refuses). */
    public Optional<MilkCharges> take(long now, double rate, MilkSettings settings) {
        MilkCharges current = now(now, rate, settings);
        if (current.charges == 0) {
            return Optional.empty();
        }
        return Optional.of(new MilkCharges(current.charges - 1, current.progress, now));
    }
}
