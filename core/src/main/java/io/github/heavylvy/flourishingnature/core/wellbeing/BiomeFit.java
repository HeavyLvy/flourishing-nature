package io.github.heavylvy.flourishingnature.core.wellbeing;

/** How a species feels about the biome it's standing in. */
public enum BiomeFit {
    PREFERRED(1.0),
    TOLERATED(0.5),
    DISLIKED(0.1);

    private final double score;

    BiomeFit(double score) {
        this.score = score;
    }

    public NeedScore score() {
        return new NeedScore(score);
    }
}
