package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.ChaseSpeed;
import io.github.heavylvy.flourishingnature.core.herd.FleeRoute;
import io.github.heavylvy.flourishingnature.core.herd.PanicAlarm;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Runs from danger after a herd mate panicked. Each animal swerves its own random way while running
 * away from the danger, so the herd scatters in all directions instead of moving as one block. Shares
 * vanilla panic's priority.
 */
final class HerdPanicGoal extends Goal {
    static final int PRIORITY = 1;
    private static final double SPEED = 1.8;

    private final Animal animal;
    private Optional<Position> escape = Optional.empty();

    HerdPanicGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private Optional<PanicAlarm> activeAlarm() {
        long now = animal.level().getGameTime();
        return animal.getExistingData(HerdAttachments.ALARM).filter(alarm -> alarm.isActive(now));
    }

    @Override
    public boolean canUse() {
        if (EntityFacts.isLeashed(animal) || animal.isPassenger()) {
            return false;
        }
        escape = activeAlarm().flatMap(this::findEscape);
        return escape.isPresent();
    }

    private Optional<Position> findEscape(PanicAlarm alarm) {
        return Optional.of(FleeRoute.pick(EntityFacts.position(animal), alarm.source(), new McRandomness(animal.getRandom())));
    }

    @Override
    public void start() {
        double speed = activeAlarm()
                .map(alarm -> ChaseSpeed.fleeModifier(
                        animal.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), alarm.chaserSpeed(), SPEED))
                .orElse(SPEED);
        escape.ifPresent(e -> animal.getNavigation().moveTo(e.x(), e.y(), e.z(), speed));
    }

    @Override
    public boolean canContinueToUse() {
        return !animal.getNavigation().isDone() && activeAlarm().isPresent();
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        escape = Optional.empty();
    }
}
