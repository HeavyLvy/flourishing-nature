package io.github.heavylvy.flourishingnature.neoforge.nest;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;

/** When an egg is due, the hen walks to her nest and lays it there instead of on the ground. */
final class LayInNestGoal extends Goal {
    static final int PRIORITY = 3;
    private static final int GIVE_UP_TICKS = 600;

    private final Chicken hen;
    private int ticks;

    LayInNestGoal(Chicken hen) {
        this.hen = hen;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean wanted() {
        return !hen.isBaby() && HenNests.hasEggToLay(hen)
                && HenNests.nest(hen).filter(nest -> !nest.clutch().isFull()).isPresent();
    }

    @Override
    public boolean canUse() {
        return hen.tickCount % 10 == 0 && wanted();
    }

    @Override
    public void start() {
        ticks = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < GIVE_UP_TICKS && wanted();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        HenNests.nest(hen).ifPresent(nest -> {
            if (NestWalk.stepOnto(hen, nest.getBlockPos())) {
                HenNests.layInto(hen, nest);
            }
        });
    }

    @Override
    public void stop() {
        hen.getNavigation().stop();
    }
}
