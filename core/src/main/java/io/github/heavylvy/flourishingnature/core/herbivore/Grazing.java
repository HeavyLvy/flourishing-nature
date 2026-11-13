package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** When an animal grazes and which food it picks. */
public final class Grazing {
    private Grazing() {}

    /** Only an animal that isn't full looks for food. */
    public static boolean wantsToEat(Fullness fullness, GrazingSettings settings) {
        return fullness.value() < settings.grazeBelow();
    }

    /**
     * Cost of walking to a candidate: distance divided by appetite, plus a step so a favourite a
     * couple of blocks further still wins over food right underfoot.
     */
    public static double cost(Diet diet, ForageCandidate candidate) {
        double appetite = diet.appetite(candidate.food()).map(Appetite::value).orElse(0.0);
        return appetite > 0.0 ? (candidate.distance() + 1.0) / appetite : Double.POSITIVE_INFINITY;
    }

    /** The cheapest candidate this diet eats, if any. */
    public static <T extends HasCandidate> Optional<T> best(Diet diet, List<T> options) {
        return options.stream()
                .filter(o -> diet.eats(o.candidate().food()))
                .min(Comparator.comparingDouble(o -> cost(diet, o.candidate())));
    }

    /** Whether this animal crops the grass block under it now. */
    public static boolean cropsGrass(Diet diet, Fullness fullness, GrazingSettings settings) {
        return switch (diet.cropsGrassBlocks()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case WHEN_VERY_HUNGRY -> fullness.value() < settings.veryHungryBelow();
        };
    }

    /** Something carrying a forage candidate, so the loader can attach its own block or item. */
    public interface HasCandidate {
        ForageCandidate candidate();
    }
}
