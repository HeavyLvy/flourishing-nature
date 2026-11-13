package io.github.heavylvy.flourishingnature.core.land;

/**
 * How many random spots are looked at, and how often. Like vanilla's random ticks this only
 * touches chunks near players, so the cost is bounded by loaded chunks x samples / interval.
 *
 * @param intervalTicks sample every this many ticks
 * @param samplesPerChunk random columns per loaded chunk each time
 */
public record Sampler(int intervalTicks, int samplesPerChunk) {
    public static final int MAX_SAMPLES = 16;

    public static final Sampler DEFAULT = new Sampler(4, 1);

    public Sampler {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("interval must be >= 1 tick: " + intervalTicks);
        }
        if (samplesPerChunk < 0 || samplesPerChunk > MAX_SAMPLES) {
            throw new IllegalArgumentException("samples per chunk must be in [0, " + MAX_SAMPLES + "]: " + samplesPerChunk);
        }
    }

    public boolean runsAt(long gameTime) {
        return samplesPerChunk > 0 && gameTime % intervalTicks == 0;
    }

    /** Average ticks between two samples of the same column (a chunk has 256 columns). */
    public double ticksPerColumnSample() {
        return samplesPerChunk == 0 ? Double.POSITIVE_INFINITY : 256.0 * intervalTicks / samplesPerChunk;
    }
}
