package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.herbivore.Diet;
import io.github.heavylvy.flourishingnature.core.herbivore.Grazing;
import io.github.heavylvy.flourishingnature.core.herbivore.GrazingSettings;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

/**
 * A hungry animal walks to a plant or dropped food it eats and eats it: the plant is gone, its
 * fullness goes up. Searches only every few seconds and only while it isn't full.
 */
final class GrazeGoal extends Goal {
    static final int PRIORITY = 4;
    /** Rabbits keep their vanilla garden raid at 5 and stroll at 6. */
    static final int RABBIT_PRIORITY = 5;
    private static final double SPEED = 1.0;
    private static final double REACH = 1.6;
    private static final int GIVE_UP_TICKS = 200;
    private static final int CHEW_TICKS = 10;
    /** After a meal it looks again soon, so a hungry animal grazes several plants in a row. */
    private static final int AFTER_MEAL_DELAY = 20;

    private final Animal animal;
    private final Diet diet;
    private Aim aim = Aim.NONE;
    private long nextSearch;
    private int ticks;
    private int chewing;

    /** What the goal is walking to; a sealed stand-in for an optional field. */
    private sealed interface Aim {
        Aim NONE = new Nothing();

        record Nothing() implements Aim {}

        record At(ForageTarget target) implements Aim {}
    }

    GrazeGoal(Animal animal, Diet diet) {
        this.animal = animal;
        this.diet = diet;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(animal.level() instanceof ServerLevel level) || !HerbivoreConfig.grazing()
                || animal.isVehicle() || EntityFacts.isLeashed(animal)) {
            return false;
        }
        long now = level.getGameTime();
        if (now < nextSearch) {
            return false;
        }
        GrazingSettings settings = HerbivoreConfig.grazingSettings();
        nextSearch = now + settings.searchInterval() + animal.getRandom().nextInt(20);
        if (!Grazing.wantsToEat(FullnessApi.fullness(animal), settings)) {
            return false;
        }
        Optional<ForageTarget> found = find(level, settings);
        aim = found.<Aim>map(Aim.At::new).orElse(Aim.NONE);
        return found.isPresent();
    }

    /** The cheapest food the filters allow; filters run on candidates in cost order until one passes. */
    private Optional<ForageTarget> find(ServerLevel level, GrazingSettings settings) {
        List<ForageTarget> options = Forage.around(level, animal.position(), diet, settings.searchRadius());
        return options.stream()
                .sorted(Comparator.comparingDouble(t -> Grazing.cost(diet, t.candidate())))
                .filter(t -> GrazingFilters.allows(level, animal, t))
                .findFirst();
    }

    /** Test hook: search on the next goal check instead of waiting. */
    void searchNow() {
        nextSearch = 0L;
    }

    @Override
    public void start() {
        ticks = 0;
        chewing = 0;
        walk();
    }

    private void walk() {
        if (aim instanceof Aim.At(ForageTarget target)) {
            Vec3 where = target.where();
            animal.getNavigation().moveTo(where.x, where.y, where.z, SPEED);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return aim instanceof Aim.At(ForageTarget target) && ticks < GIVE_UP_TICKS && stillThere(target);
    }

    @Override
    public void tick() {
        if (!(aim instanceof Aim.At(ForageTarget target))) {
            return;
        }
        ticks++;
        Vec3 where = target.where();
        animal.getLookControl().setLookAt(where.x, where.y, where.z);
        double dx = animal.getX() - where.x;
        double dz = animal.getZ() - where.z;
        boolean close = dx * dx + dz * dz <= REACH * REACH && Math.abs(animal.getY() - where.y) <= 1.5;
        if (!close) {
            chewing = 0;
            if (animal.getNavigation().isDone() && ticks % 20 == 0) {
                walk();
            }
            return;
        }
        animal.getNavigation().stop();
        if (++chewing >= CHEW_TICKS) {
            eat(target);
            aim = Aim.NONE;
            nextSearch = animal.level().getGameTime() + AFTER_MEAL_DELAY;
        }
    }

    private boolean stillThere(ForageTarget target) {
        return switch (target) {
            case ForageTarget.Plant plant -> Forage.food(animal.level().getBlockState(plant.pos())).equals(Optional.of(plant.food()));
            case ForageTarget.Dropped dropped -> dropped.item().isAlive() && !dropped.item().getItem().isEmpty();
        };
    }

    private void eat(ForageTarget target) {
        if (!(animal.level() instanceof ServerLevel level) || !stillThere(target)) {
            return;
        }
        switch (target) {
            case ForageTarget.Plant plant -> {
                if (!Protection.mayEdit(level, animal, plant.pos())) {
                    return;
                }
                level.destroyBlock(plant.pos(), false, animal);
            }
            case ForageTarget.Dropped dropped -> {
                dropped.item().getItem().shrink(1);
                if (dropped.item().getItem().isEmpty()) {
                    dropped.item().discard();
                }
                animal.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.0F);
            }
        }
        FullnessApi.eat(animal, Diet.meal(target.food()));
    }

    @Override
    public void stop() {
        aim = Aim.NONE;
        animal.getNavigation().stop();
    }
}
