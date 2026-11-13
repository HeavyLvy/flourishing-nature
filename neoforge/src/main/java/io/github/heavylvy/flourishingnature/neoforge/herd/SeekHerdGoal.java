package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdDecision;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Walks toward a herd when the animal has too few mates, or drifts away when it has too many. Sits
 * below panic, breeding, tempting and following parents, and above random strolling.
 */
final class SeekHerdGoal extends Goal {
    static final int PRIORITY = 4;
    private static final double SPEED = 1.0;
    private static final double ARRIVED_DISTANCE = 2.0;
    private static final int MAX_DURATION_TICKS = 200;

    private final Animal animal;
    private final HerdBrain brain;
    private Optional<Position> target = Optional.empty();
    private int runningTicks;

    SeekHerdGoal(Animal animal, HerdBrain brain) {
        this.animal = animal;
        this.brain = brain;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!brain.canHerd()) {
            return false;
        }
        target = switch (brain.refresh().decision()) {
            case HerdDecision.SeekHerd seek -> Optional.of(seek.target());
            case HerdDecision.Disperse disperse -> Optional.of(disperse.target());
            case HerdDecision.Content content -> Optional.empty();
            case HerdDecision.Lonely lonely -> Optional.empty();
        };
        Position here = EntityFacts.position(animal);
        return target.map(t -> t.distanceTo(here) > ARRIVED_DISTANCE).orElse(false);
    }

    @Override
    public void start() {
        runningTicks = 0;
        target.ifPresent(t -> animal.getNavigation().moveTo(t.x(), t.y(), t.z(), SPEED));
    }

    @Override
    public boolean canContinueToUse() {
        return brain.canHerd() && !animal.getNavigation().isDone() && runningTicks < MAX_DURATION_TICKS;
    }

    @Override
    public void tick() {
        runningTicks++;
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        target = Optional.empty();
    }
}
