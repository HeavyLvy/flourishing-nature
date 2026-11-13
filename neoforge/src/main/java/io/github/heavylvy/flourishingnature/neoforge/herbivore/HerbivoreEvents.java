package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.herbivore.Diet;
import io.github.heavylvy.flourishingnature.core.herbivore.Diets;
import io.github.heavylvy.flourishingnature.core.herbivore.Food;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdManager;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Wires grazing, self-breeding, food-seeking homes and danger memory onto vanilla animals. */
public final class HerbivoreEvents {
    private HerbivoreEvents() {}

    /** Sheep never eat the last flower of a kind within this many blocks. */
    static final int LAST_FLOWER_REACH = 8;

    public static void register(IEventBus modBus, IEventBus gameBus) {
        HerbivoreAttachments.REGISTER.register(modBus);
        gameBus.addListener(HerbivoreEvents::onJoin);
        gameBus.addListener(SelfBreedingCheck::onTick);
        gameBus.addListener(DangerMemories::onDamaged);
        HerdManager.addHomeAdjuster(ForagingHomes::adjust);
        GrazingFilters.register(HerbivoreEvents::notInABase);
        GrazingFilters.register(HerbivoreEvents::insideGrazingCircle);
        GrazingFilters.register(HerbivoreEvents::sheepSpareLastFlower);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Animal animal)) {
            return;
        }
        Diets.forSpecies(EntityFacts.species(animal)).ifPresent(diet -> addGoals(animal, diet));
    }

    private static void addGoals(Animal animal, Diet diet) {
        animal.goalSelector.addGoal(animal instanceof Rabbit ? GrazeGoal.RABBIT_PRIORITY : GrazeGoal.PRIORITY, new GrazeGoal(animal, diet));
        if (diet.cropsGrassBlocks() != Diet.GrassCropping.NEVER) {
            if (animal instanceof Sheep) {
                animal.goalSelector.removeAllGoals(goal -> goal instanceof EatBlockGoal);
            }
            animal.goalSelector.addGoal(CropGrassGoal.PRIORITY, new CropGrassGoal(animal, diet));
        }
    }

    /** Plants in lit bases (block light 8+) are left alone; food on the ground is fair game. */
    private static boolean notInABase(ServerLevel level, Animal animal, ForageTarget target) {
        return switch (target) {
            case ForageTarget.Plant plant -> Protection.mayEdit(level, animal, plant.pos());
            case ForageTarget.Dropped dropped -> true;
        };
    }

    /** Herd members eat inside their herd's grazing circle, so the herd doesn't pull them back mid-walk. */
    private static boolean insideGrazingCircle(ServerLevel level, Animal animal, ForageTarget target) {
        Optional<HerdId> herd = animal.getExistingData(HerdAttachments.HERD);
        return herd.flatMap(h -> HerdManager.homeOf(level, h).map(home -> {
            Vec3 where = target.where();
            double dx = where.x - home.anchor().x();
            double dz = where.z - home.anchor().z();
            double reach = HerdManager.grazingRadius(level, h) + 1.0;
            return dx * dx + dz * dz <= reach * reach;
        })).orElse(true);
    }

    /** The sheep gardener's brake: never eat the last flower of its kind within 8 blocks. */
    private static boolean sheepSpareLastFlower(ServerLevel level, Animal animal, ForageTarget target) {
        if (!(animal instanceof Sheep) || !(target instanceof ForageTarget.Plant plant) || plant.food() != Food.FLOWER) {
            return true;
        }
        BlockState flower = level.getBlockState(plant.pos());
        BlockPos c = plant.pos();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-LAST_FLOWER_REACH, -3, -LAST_FLOWER_REACH),
                c.offset(LAST_FLOWER_REACH, 3, LAST_FLOWER_REACH))) {
            if (!p.equals(c) && level.getBlockState(p).is(flower.getBlock())) {
                return true;
            }
        }
        return false;
    }
}
