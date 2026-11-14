package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.core.nest.NestRules;
import io.github.heavylvy.flourishingnature.core.nest.NestSettings;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;

/**
 * A hen with fertile eggs in her nest sits on them in long bouts, with short breaks between to eat
 * and stretch. Sitting is what warms the eggs (see {@link NestBlockEntity#check}).
 */
final class BroodGoal extends Goal {
    static final int PRIORITY = 3;
    private static final int CHECK_INTERVAL = 100;
    /** A break after each bout, so she still eats. */
    private static final int BREAK_TICKS = 600;

    private final Chicken hen;
    private int boutLeft;
    private int cooldown;

    BroodGoal(Chicken hen) {
        this.hen = hen;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    private boolean fertileNest() {
        return !hen.isBaby() && HenNests.nest(hen).filter(nest -> nest.clutch().hasFertile()).isPresent();
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = adjustedTickDelay(CHECK_INTERVAL);
        NestSettings settings = NestConfig.settings();
        return fertileNest() && NestRules.startsBrooding(true, new McRandomness(hen.getRandom()), settings);
    }

    @Override
    public void start() {
        boutLeft = NestConfig.settings().broodBoutTicks();
    }

    @Override
    public boolean canContinueToUse() {
        return boutLeft > 0 && fertileNest();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        boutLeft--;
        HenNests.nest(hen).ifPresent(nest -> NestWalk.stepOnto(hen, nest.getBlockPos()));
    }

    @Override
    public void stop() {
        hen.getNavigation().stop();
        cooldown = adjustedTickDelay(BREAK_TICKS);
    }
}
