package io.github.heavylvy.flourishingnature.neoforge.weather;

import io.github.heavylvy.flourishingnature.neoforge.boundary.Weather;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * When rain or snow starts, walk to the nearest cover (a tree, an overhang, a cave mouth) and stay
 * there until it stops. Holding the move flag while sheltered keeps strolling and herd-following
 * from dragging the animal back out; once the weather clears, the herd goals bring it home.
 * Panic and breeding still win.
 */
public final class SeekShelterGoal extends Goal {
    public static final int PRIORITY = 3;
    private static final int SEARCH_RADIUS = 14;
    private static final double SPEED = 1.1;
    private static final int SEARCH_INTERVAL = 40;

    private final Animal animal;
    private Optional<BlockPos> shelter = Optional.empty();
    private int cooldown;

    public SeekShelterGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean wet() {
        return Weather.precipitating(animal.level(), animal.blockPosition()) && !animal.isLeashed() && !animal.isPassenger();
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = adjustedTickDelay(SEARCH_INTERVAL);
        if (!wet()) {
            return false;
        }
        if (Weather.covered(animal)) {
            shelter = Optional.of(animal.blockPosition());
            return true;
        }
        shelter = Weather.nearestShelter(animal, SEARCH_RADIUS);
        return shelter.isPresent();
    }

    @Override
    public void start() {
        shelter.filter(spot -> !spot.equals(animal.blockPosition()))
                .ifPresent(spot -> animal.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, SPEED));
    }

    /** Keep going while it's wet and we're either on the way or already under cover. */
    @Override
    public boolean canContinueToUse() {
        return wet() && (!animal.getNavigation().isDone() || Weather.covered(animal));
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        shelter = Optional.empty();
    }
}
