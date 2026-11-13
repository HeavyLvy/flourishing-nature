package io.github.heavylvy.flourishingnature.core.nest;

/** One egg in a nest: an ordinary egg, or a fertile one that hatches once it has been kept warm. */
public sealed interface NestEgg {
    /** An ordinary egg. Never hatches. */
    record Plain() implements NestEgg {}

    /**
     * A fertile egg from breeding.
     *
     * @param warmth game ticks a hen has sat on it so far; pauses while no hen sits, never resets
     */
    record Fertile(long warmth) implements NestEgg {
        public Fertile {
            if (warmth < 0) {
                throw new IllegalArgumentException("warmth must be >= 0: " + warmth);
            }
        }

        public static Fertile fresh() {
            return new Fertile(0);
        }

        Fertile warmedBy(long ticks) {
            return new Fertile(warmth + ticks);
        }
    }
}
