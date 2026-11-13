package io.github.heavylvy.flourishingnature.core.predator;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.ArrayList;
import java.util.List;

/**
 * Pack-level decisions (design doc, "Predators follow the food" and "Pack dynamics"). A pack is a
 * herd of wild wolves; these rules only look at its members' fullness and memories.
 */
public final class PackRules {
    private PackRules() {}

    /** The pack eats together, so its hunger is its members' average. */
    public static Fullness packFullness(List<Fullness> members) {
        if (members.isEmpty()) {
            throw new IllegalArgumentException("a pack has members");
        }
        double sum = 0;
        for (Fullness f : members) {
            sum += f.value();
        }
        return new Fullness(sum / members.size());
    }

    /**
     * The pack's next home when it smells prey: one step toward it, stopping a little short so
     * the wolves spread around the prey rather than stand on it.
     */
    public static Position driftToward(Position anchor, Position prey, PackSettings s) {
        double dx = prey.x() - anchor.x();
        double dz = prey.z() - anchor.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double step = Math.min(s.driftStep(), distance - s.driftStopWithin());
        if (step <= 0) {
            return anchor;
        }
        return new Position(anchor.x() + dx / distance * step, prey.y(), anchor.z() + dz / distance * step);
    }

    /**
     * A full pack with two adults ready to breed, and room for a pup, has one.
     *
     * @param readyAdults adults that are full, grown, and past their own breeding cooldown
     * @param packSize everyone in the pack, pups included
     */
    public static boolean mayBreed(int readyAdults, int packSize, HungerBand packHunger, PackSettings s) {
        return packHunger == HungerBand.FULL && readyAdults >= 2 && packSize < s.maxBreedingPackSize();
    }

    /** Whether this wolf has waited long enough since its last pup. */
    public static boolean pastBreedCooldown(PackMemory memory, long now, PackSettings s) {
        return now - memory.lastBred() >= s.breedCooldown();
    }

    /**
     * A pack breaks up when every adult is starving and none has eaten for {@code splitAfter}.
     *
     * @param adultHunger hunger of each adult member
     * @param lastPackMeal the most recent meal of any member
     */
    public static boolean shouldSplit(List<HungerBand> adultHunger, long lastPackMeal, long now, PackSettings s) {
        if (adultHunger.isEmpty() || now - lastPackMeal < s.splitAfter()) {
            return false;
        }
        for (HungerBand band : adultHunger) {
            if (band != HungerBand.STARVING) {
                return false;
            }
        }
        return true;
    }

    /**
     * Where each member of a broken pack heads: evenly spread directions (with a random turn), so
     * the wolves really go separate ways, each far enough to be out of reach of the old pack.
     */
    public static List<Position> dispersalTargets(Position center, int count, Randomness random, PackSettings s) {
        List<Position> targets = new ArrayList<>();
        double offset = random.nextDouble() * 2 * Math.PI;
        for (int i = 0; i < count; i++) {
            double angle = offset + 2 * Math.PI * i / count + (random.nextDouble() - 0.5) * Math.PI / count;
            double distance = s.dispersalMin() + random.nextDouble() * (s.dispersalMax() - s.dispersalMin());
            targets.add(new Position(
                    center.x() + Math.cos(angle) * distance, center.y(), center.z() + Math.sin(angle) * distance));
        }
        return targets;
    }
}
