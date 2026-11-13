package io.github.heavylvy.flourishingnature.neoforge.production;

import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Slower wool regrowth. Vanilla regrows a sheared sheep's wool every time it eats grass. Here each
 * regrowth only sticks with chance {@link ProductionRules#woolRegrowChance}: half the time for a
 * hungry sheep, a third for a stressed one, never for a starving or miserable one; otherwise the
 * sheep stays sheared until a later meal. Spotted as the sheared flag flipping between ticks, so it
 * works whatever made the sheep eat.
 */
public final class WoolGrowth {
    private WoolGrowth() {}

    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep) || sheep.level().isClientSide()) {
            return;
        }
        boolean sheared = sheep.isSheared();
        boolean wasSheared = sheep.getData(ProductionAttachments.WAS_SHEARED);
        if (wasSheared && !sheared && ProductionConfig.wool() && AnimalCondition.tracked(sheep)) {
            AnimalCondition condition = AnimalCondition.of(sheep);
            if (sheep.getRandom().nextDouble() >= ProductionRules.woolRegrowChance(condition.mood(), condition.hunger())) {
                sheep.setSheared(true);
                sheared = true;
            }
        }
        if (sheared != wasSheared) {
            sheep.setData(ProductionAttachments.WAS_SHEARED, sheared);
        }
    }
}
