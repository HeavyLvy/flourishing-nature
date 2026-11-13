package io.github.heavylvy.flourishingnature.neoforge.boundary;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import net.minecraft.util.RandomSource;

/** Adapts Minecraft's {@link RandomSource} to core's {@link Randomness}. */
public record McRandomness(RandomSource source) implements Randomness {
    @Override
    public double nextDouble() {
        return source.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return source.nextGaussian();
    }

    @Override
    public int nextIntInclusive(int min, int max) {
        return source.nextIntBetweenInclusive(min, max);
    }
}
