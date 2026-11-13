package io.github.heavylvy.flourishingnature.core.random;

/** Random numbers. Loader modules adapt the game's own random source to this. */
public interface Randomness {
    /** A uniform double in [0, 1). */
    double nextDouble();

    /** A normally distributed double with mean 0 and standard deviation 1. */
    double nextGaussian();

    /** A uniform int in [min, max], both inclusive. */
    int nextIntInclusive(int min, int max);
}
