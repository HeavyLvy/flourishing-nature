package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.Cohesion;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McTerrain;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Light flocking inside the herd: step aside when a herd mate is too close, close up when the
 * nearest one has drifted off. Only counts its own herd, and never steps into water or a cave.
 * Shares a priority with vanilla's random stroll so the two take turns.
 */
final class HerdCohesionGoal extends Goal {
    static final int PRIORITY = 5;
    private static final double SPEED = 0.8;
    private static final int CHECK_INTERVAL = 20;

    private final Animal animal;
    private final HerdBrain brain;
    private Optional<Position> target = Optional.empty();
    private int cooldown;

    HerdCohesionGoal(Animal animal, HerdBrain brain) {
        this.animal = animal;
        this.brain = brain;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = adjustedTickDelay(CHECK_INTERVAL);
        if (!brain.canHerd()) {
            return false;
        }
        McTerrain terrain = new McTerrain(animal.level());
        target = brain.self()
                .flatMap(me -> Cohesion.adjust(me, HerdLookup.herdMates(animal, brain.settings().herdRadius()), brain.settings()))
                .flatMap(terrain::pasture);
        return target.isPresent();
    }

    @Override
    public void start() {
        target.ifPresent(t -> animal.getNavigation().moveTo(t.x(), t.y(), t.z(), SPEED));
    }

    @Override
    public boolean canContinueToUse() {
        return brain.canHerd() && !animal.getNavigation().isDone();
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        target = Optional.empty();
    }
}
