package io.github.heavylvy.flourishingnature.core.production;

import java.util.ArrayList;
import java.util.List;

/**
 * How {@link ProductionRules#dropRollBonus} and {@link ProductionRules#minimumDropsOnly} change a
 * list of drops. Results keep the input's order and length so callers can map them back; a stack
 * with count 0 is dropped.
 */
public final class DropRules {
    private DropRules() {}

    /**
     * One roll fewer per point of {@code fewer}: each stack loses that many items. An animal that
     * dropped meat still drops at least one, so a hungry animal never gives less than a starving one.
     */
    public static List<DropStack> fewerRolls(List<DropStack> drops, int fewer) {
        if (fewer < 0) {
            throw new IllegalArgumentException("fewer must be 0 or more: " + fewer);
        }
        List<DropStack> out = new ArrayList<>(drops.size());
        boolean hadMeat = false;
        boolean keptMeat = false;
        for (DropStack drop : drops) {
            DropStack less = drop.withCount(Math.max(0, drop.count() - fewer));
            hadMeat |= drop.meat() && drop.count() > 0;
            keptMeat |= less.meat() && less.count() > 0;
            out.add(less);
        }
        if (hadMeat && !keptMeat) {
            return minimum(drops);
        }
        return List.copyOf(out);
    }

    /** The minimum: one meat from the first meat stack, nothing else. */
    public static List<DropStack> minimum(List<DropStack> drops) {
        List<DropStack> out = new ArrayList<>(drops.size());
        boolean given = false;
        for (DropStack drop : drops) {
            boolean give = !given && drop.meat() && drop.count() > 0;
            given |= give;
            out.add(drop.withCount(give ? 1 : 0));
        }
        return List.copyOf(out);
    }
}
