package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McTerrain;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

/**
 * Keeps an animal near its herd's home. When it has strayed outside the herd's grazing circle, it
 * walks to a dry, open spot inside it. When a roaming herd moves its home, everyone follows; when
 * a horse herd gallops, everyone runs to keep up.
 */
final class FollowHerdGoal extends Goal {
    static final int PRIORITY = 4;
    private static final double SPEED = 1.0;
    private static final double CATCH_UP_SPEED = 1.15;
    /** Running with a galloping herd. */
    private static final double GALLOP_SPEED = 2.2;
    private static final double GALLOP_SPREAD = 3.0;
    private static final int CHECK_INTERVAL = 20;

    private final Animal animal;
    private final HerdBrain brain;
    private Optional<Position> target = Optional.empty();
    private double speed = SPEED;
    private int cooldown;

    FollowHerdGoal(Animal animal, HerdBrain brain) {
        this.animal = animal;
        this.brain = brain;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Grazing circle radius: bigger herds spread out more. */
    static double spread(int herdSize) {
        return 3.0 + 1.2 * Math.sqrt(Math.max(1, herdSize));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0 && !brain.isGalloping()) {
            cooldown--;
            return false;
        }
        cooldown = adjustedTickDelay(CHECK_INTERVAL);
        if (!brain.canHerd()) {
            return false;
        }
        Optional<HerdHome> home = brain.home();
        Position here = EntityFacts.position(animal);
        boolean galloping = brain.isGalloping();
        double spread = galloping ? GALLOP_SPREAD : spread(brain.herdSize());
        target = home.filter(h -> h.anchor().distanceTo(here) > spread)
                .flatMap(h -> pickSpot(h.anchor(), spread));
        speed = galloping
                ? GALLOP_SPEED
                : home.map(h -> h.anchor().distanceTo(here) > 3 * spread ? CATCH_UP_SPEED : SPEED).orElse(SPEED);
        return target.isPresent();
    }

    /** A random dry spot within half the grazing circle, or the anchor itself. */
    private Optional<Position> pickSpot(Position anchor, double spread) {
        McTerrain terrain = new McTerrain(animal.level());
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = animal.getRandom().nextDouble() * 2 * Math.PI;
            double r = animal.getRandom().nextDouble() * spread / 2;
            Optional<Position> spot = terrain.pasture(
                    new Position(anchor.x() + Math.cos(angle) * r, anchor.y(), anchor.z() + Math.sin(angle) * r));
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.of(anchor);
    }

    @Override
    public void start() {
        target.ifPresent(t -> animal.getNavigation().moveTo(t.x(), t.y(), t.z(), speed));
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
