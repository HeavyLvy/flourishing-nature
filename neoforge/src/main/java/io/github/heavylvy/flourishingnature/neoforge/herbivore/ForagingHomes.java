package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerSettings;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerSpot;
import io.github.heavylvy.flourishingnature.core.herbivore.Diet;
import io.github.heavylvy.flourishingnature.core.herbivore.Diets;
import io.github.heavylvy.flourishingnature.core.herbivore.FoodPatch;
import io.github.heavylvy.flourishingnature.core.herbivore.Grazing;
import io.github.heavylvy.flourishingnature.core.herbivore.HomeChoice;
import io.github.heavylvy.flourishingnature.core.herbivore.HomeSettings;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McTerrain;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * After each herd planning pass: a herd whose home sits where a member was attacked moves out of
 * the danger area, and a hungry herd whose home has no food left moves to the richest patch nearby.
 * One per level.
 */
public final class ForagingHomes {
    /** Members are looked for this far from home (the herd planner's leash). */
    private static final double MEMBER_REACH = 40.0;

    private final Map<HerdId, Long> nextFoodCheck = new HashMap<>();

    static ForagingHomes of(ServerLevel level) {
        return level.getData(HerbivoreAttachments.HOMES);
    }

    /** Test hook: the herd checks its food on the next planning pass instead of waiting. */
    public static void checkFoodSoon(ServerLevel level, HerdId herd) {
        of(level).nextFoodCheck.put(herd, 0L);
    }

    static HerdHome adjust(ServerLevel level, HerdId herd, Animal leader, HerdHome home) {
        return of(level).update(level, herd, leader, home);
    }

    private HerdHome update(ServerLevel level, HerdId herd, Animal leader, HerdHome home) {
        long now = level.getGameTime();
        List<Animal> members = members(level, herd, home.anchor());
        DangerSettings danger = HerbivoreConfig.dangerSettings();
        List<DangerSpot> dangers = new ArrayList<>();
        if (HerbivoreConfig.dangerMemory()) {
            members.forEach(m -> dangers.addAll(DangerMemories.of(m).active(now, danger)));
        }
        McTerrain terrain = new McTerrain(level);
        HerdHome adjusted = HomeChoice.avoidDanger(home, dangers, now, terrain, danger);
        Optional<Diet> diet = Diets.forSpecies(EntityFacts.species(leader));
        HomeSettings settings = HerbivoreConfig.homeSettings();
        if (!HerbivoreConfig.followFood() || diet.isEmpty() || now < nextFoodCheck.getOrDefault(herd, 0L)) {
            return adjusted;
        }
        nextFoodCheck.put(herd, now + settings.foodCheckInterval());
        nextFoodCheck.values().removeIf(at -> at < now - 10L * settings.foodCheckInterval());
        boolean hungry = members.stream()
                .mapToDouble(m -> FullnessApi.fullness(m).value())
                .average()
                .stream()
                .anyMatch(f -> Grazing.wantsToEat(new Fullness(f),
                        HerbivoreConfig.grazingSettings()));
        if (!hungry) {
            return adjusted;
        }
        Position anchor = adjusted.anchor();
        int atHome = Forage.ediblePlants(level, new Vec3(anchor.x(), anchor.y(), anchor.z()), diet.get(),
                HerbivoreConfig.grazingSettings().ediblePlantsReach());
        if (atHome > settings.scarceAt()) {
            return adjusted;
        }
        List<FoodPatch> patches = new ArrayList<>();
        for (Position centre : HomeChoice.patchCentres(anchor, settings)) {
            BlockPos pos = BlockPos.containing(centre.x(), centre.y(), centre.z());
            if (level.isLoaded(pos)) {
                patches.add(new FoodPatch(centre, Forage.patchFood(level, pos, diet.get(), settings.patchHalfWidth())));
            }
        }
        return HomeChoice.followFood(adjusted, atHome, patches, dangers, now, terrain, danger, settings);
    }

    private static List<Animal> members(ServerLevel level, HerdId herd, Position anchor) {
        AABB box = AABB.ofSize(new Vec3(anchor.x(), anchor.y(), anchor.z()), MEMBER_REACH * 2, MEMBER_REACH, MEMBER_REACH * 2);
        return level.getEntitiesOfClass(Animal.class, box,
                a -> a.isAlive() && a.getExistingData(HerdAttachments.HERD).equals(Optional.of(herd)));
    }
}
