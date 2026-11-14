package io.github.heavylvy.flourishingnature.neoforge.nest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.chicken.Chicken;

/** Getting a hen onto a nest or next to a target block. */
final class NestWalk {
    private NestWalk() {}

    static final double SPEED = 1.0;
    /** Within this many blocks the hen steps in directly instead of pathing. */
    private static final double STEP_IN = 2.0;

    /** Keeps her heading for the middle of {@code pos}; true once she stands in that block. */
    static boolean stepOnto(Chicken hen, BlockPos pos) {
        if (hen.blockPosition().equals(pos)) {
            hen.getNavigation().stop();
            return true;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (hen.distanceToSqr(x, pos.getY(), z) < STEP_IN * STEP_IN) {
            hen.getNavigation().stop();
            hen.getMoveControl().setWantedPosition(x, pos.getY(), z, SPEED);
        } else if (hen.getNavigation().isDone()) {
            hen.getNavigation().moveTo(x, pos.getY(), z, SPEED);
        }
        return false;
    }

    /** Keeps her heading for a point; true once she is within {@code reach} of it. */
    static boolean reach(Chicken hen, double x, double y, double z, double reach) {
        double distance = hen.distanceToSqr(x, y, z);
        if (distance <= reach * reach) {
            hen.getNavigation().stop();
            return true;
        }
        if (distance < STEP_IN * STEP_IN * 4) {
            // Paths end at the nearest block centre, which can leave her just out of reach.
            hen.getNavigation().stop();
            hen.getMoveControl().setWantedPosition(x, y, z, SPEED);
        } else if (hen.getNavigation().isDone()) {
            hen.getNavigation().moveTo(x, y, z, SPEED);
        }
        return false;
    }
}
