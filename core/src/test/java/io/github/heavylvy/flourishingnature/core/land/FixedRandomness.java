package io.github.heavylvy.flourishingnature.core.land;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.Random;

/** Randomness for tests: a fixed roll, or a seeded stream. */
final class FixedRandomness implements Randomness {
    private final Random random;
    private final double fixed;

    private FixedRandomness(Random random, double fixed) {
        this.random = random;
        this.fixed = fixed;
    }

    /** Every roll returns {@code value}. */
    static FixedRandomness rolling(double value) {
        return new FixedRandomness(new Random(0), value);
    }

    static FixedRandomness seeded(long seed) {
        return new FixedRandomness(new Random(seed), -1.0);
    }

    @Override
    public double nextDouble() {
        return fixed >= 0.0 ? fixed : random.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return random.nextGaussian();
    }

    @Override
    public int nextIntInclusive(int min, int max) {
        return random.nextInt(min, max + 1);
    }
}
