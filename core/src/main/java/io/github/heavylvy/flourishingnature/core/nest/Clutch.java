package io.github.heavylvy.flourishingnature.core.nest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The eggs in one nest, oldest first. At most {@link #MAX} eggs.
 *
 * @param eggs the eggs, oldest first
 */
public record Clutch(List<NestEgg> eggs) {
    public static final int MAX = 4;
    public static final Clutch EMPTY = new Clutch(List.of());

    public Clutch {
        eggs = List.copyOf(eggs);
        if (eggs.size() > MAX) {
            throw new IllegalArgumentException("a nest holds at most " + MAX + " eggs: " + eggs.size());
        }
    }

    public int size() {
        return eggs.size();
    }

    public boolean isFull() {
        return eggs.size() >= MAX;
    }

    public boolean isEmpty() {
        return eggs.isEmpty();
    }

    public long fertileCount() {
        return eggs.stream().filter(egg -> egg instanceof NestEgg.Fertile).count();
    }

    public boolean hasFertile() {
        return fertileCount() > 0;
    }

    /** This clutch with {@code egg} added, or empty if the nest is already full. */
    public Optional<Clutch> with(NestEgg egg) {
        if (isFull()) {
            return Optional.empty();
        }
        List<NestEgg> more = new ArrayList<>(eggs);
        more.add(egg);
        return Optional.of(new Clutch(more));
    }

    /** A player takes the newest egg. Empty if there are no eggs. */
    public Optional<Taken> takeNewest() {
        if (eggs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Taken(eggs.getLast(), new Clutch(eggs.subList(0, eggs.size() - 1))));
    }

    /**
     * A hen kept the nest warm for {@code ticks}: every fertile egg's warmth grows by that much,
     * and those that reach {@code hatchAt} hatch and leave the nest.
     */
    public Incubation incubate(long ticks, long hatchAt) {
        if (ticks < 0 || hatchAt < 1) {
            throw new IllegalArgumentException("ticks must be >= 0 and hatchAt >= 1: " + ticks + ", " + hatchAt);
        }
        List<NestEgg> kept = new ArrayList<>();
        int hatched = 0;
        for (NestEgg egg : eggs) {
            switch (egg) {
                case NestEgg.Plain plain -> kept.add(plain);
                case NestEgg.Fertile fertile -> {
                    NestEgg.Fertile warmer = fertile.warmedBy(ticks);
                    if (warmer.warmth() >= hatchAt) {
                        hatched++;
                    } else {
                        kept.add(warmer);
                    }
                }
            }
        }
        return new Incubation(new Clutch(kept), hatched);
    }

    /** The egg a player took, and what is left. */
    public record Taken(NestEgg egg, Clutch rest) {}

    /** What is left after warming, and how many chicks hatched. */
    public record Incubation(Clutch rest, int hatched) {
        public Incubation {
            if (hatched < 0) {
                throw new IllegalArgumentException("hatched must be >= 0: " + hatched);
            }
        }
    }
}
