package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.fullness.FullnessRates;
import io.github.heavylvy.flourishingnature.core.herbivore.Diet;
import io.github.heavylvy.flourishingnature.core.herbivore.Grazing;
import io.github.heavylvy.flourishingnature.core.herbivore.GrazingSettings;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Vanilla's sheep grazing (crop the grass under its feet, grass block to dirt), now feeding
 * fullness too. Replaces vanilla's sheep eat goal. Sheep do it about as often as vanilla, and more
 * when not full; cows only when very hungry.
 */
final class CropGrassGoal extends Goal {
    static final int PRIORITY = 5;
    private static final int EAT_TICKS = 40;
    private static final int BITE_AT = 4;
    /** Vanilla: a 1 in 1000 chance per check for adults, 1 in 50 for lambs. */
    private static final int VANILLA_ODDS = 1000;
    private static final int LAMB_ODDS = 50;
    private static final int HUNGRY_ODDS = 200;

    private final Animal animal;
    private final Diet diet;
    private int eatTicks;

    CropGrassGoal(Animal animal, Diet diet) {
        this.animal = animal;
        this.diet = diet;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        GrazingSettings settings = HerbivoreConfig.grazingSettings();
        if (!Grazing.cropsGrass(diet, FullnessApi.fullness(animal), settings)) {
            return false;
        }
        boolean hungry = Grazing.wantsToEat(FullnessApi.fullness(animal), settings) && HerbivoreConfig.grazing();
        int odds = animal.isBaby() ? LAMB_ODDS : hungry ? HUNGRY_ODDS : VANILLA_ODDS;
        if (animal.getRandom().nextInt(reducedTickDelay(odds)) != 0) {
            return false;
        }
        BlockPos feet = animal.blockPosition();
        return animal.level().getBlockState(feet).is(BlockTags.EDIBLE_FOR_SHEEP)
                || animal.level().getBlockState(feet.below()).is(Blocks.GRASS_BLOCK);
    }

    @Override
    public void start() {
        eatTicks = EAT_TICKS;
        if (animal instanceof Sheep) {
            // Plays the head-down animation on clients, like vanilla.
            animal.level().broadcastEntityEvent(animal, (byte) 10);
        }
        animal.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return eatTicks > 0;
    }

    @Override
    public void stop() {
        eatTicks = 0;
    }

    @Override
    public void tick() {
        eatTicks = Math.max(0, eatTicks - 1);
        if (eatTicks != BITE_AT || !(animal.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos feet = animal.blockPosition();
        BlockPos below = feet.below();
        if (level.getBlockState(feet).is(BlockTags.EDIBLE_FOR_SHEEP)) {
            if (Protection.mayEdit(level, animal, feet)) {
                level.destroyBlock(feet, false);
            }
            animal.ate();
            FullnessApi.eat(animal, FullnessRates.PLANT_MEAL);
        } else if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
            if (Protection.mayEdit(level, animal, below)) {
                level.levelEvent(2001, below, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            animal.ate();
            FullnessApi.eat(animal, FullnessRates.GRASS_BLOCK_MEAL);
        }
    }
}
