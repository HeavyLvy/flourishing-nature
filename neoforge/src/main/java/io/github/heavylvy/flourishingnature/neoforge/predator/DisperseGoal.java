package io.github.heavylvy.flourishingnature.neoforge.predator;

import io.github.heavylvy.flourishingnature.core.predator.Dispersal;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Hunting;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

/** A wolf from a broken pack trots off alone toward its new territory, a stretch at a time. */
final class DisperseGoal extends Goal {
    /** Same as pack-following: below attacking, so a lone wolf still hunts on the way. */
    static final int PRIORITY = 7;
    private static final double SPEED = 1.0;
    private static final int STRETCH = 14;

    private final Wolf wolf;
    private Vec3 next = Vec3.ZERO;

    DisperseGoal(Wolf wolf) {
        this.wolf = wolf;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private Optional<Dispersal> dispersal() {
        return wolf.isTame() ? Optional.empty() : wolf.getExistingData(PredatorAttachments.DISPERSAL);
    }

    @Override
    public boolean canUse() {
        Optional<Vec3> spot = dispersal()
                .flatMap(d -> Hunting.stepToward(wolf, STRETCH, new Vec3(d.target().x(), d.target().y(), d.target().z())));
        spot.ifPresent(s -> next = s);
        return spot.isPresent();
    }

    @Override
    public void start() {
        wolf.getNavigation().moveTo(next.x, next.y, next.z, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        return dispersal().isPresent() && !wolf.getNavigation().isDone();
    }

    @Override
    public void stop() {
        wolf.getNavigation().stop();
    }
}
