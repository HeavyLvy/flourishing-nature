package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

/**
 * A target goal whose choice of target comes from our own rules. Lives in the boundary because
 * vanilla keeps the chosen target in a nullable field.
 */
public final class HuntTargetGoal extends TargetGoal {
    private final Function<Mob, Optional<LivingEntity>> choose;
    private final int randomInterval;

    /**
     * @param choose picks the prey, or nothing if the mob should not hunt now
     * @param randomInterval on average looks for prey once per this many ticks, like vanilla
     */
    public HuntTargetGoal(Mob mob, Function<Mob, Optional<LivingEntity>> choose, int randomInterval) {
        super(mob, false);
        this.choose = choose;
        this.randomInterval = reducedTickDelay(randomInterval);
        setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (randomInterval > 0 && mob.getRandom().nextInt(randomInterval) != 0) {
            return false;
        }
        Optional<LivingEntity> prey = choose.apply(mob);
        prey.ifPresent(p -> targetMob = p);
        return prey.isPresent();
    }

    @Override
    public void start() {
        mob.setTarget(targetMob);
        super.start();
    }
}
