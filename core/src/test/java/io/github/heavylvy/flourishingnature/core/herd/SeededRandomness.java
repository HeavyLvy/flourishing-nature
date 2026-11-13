package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.Random;

/** Deterministic randomness for tests. */
final class SeededRandomness implements Randomness {
    private final Random random;

    SeededRandomness(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
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
