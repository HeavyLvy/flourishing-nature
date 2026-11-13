package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.HerdAnimal;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlan;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlanSettings;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlanner;
import io.github.heavylvy.flourishingnature.core.herd.Membership;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McTerrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * One per level: runs the herd planner every {@link #PLAN_INTERVAL} ticks over every loaded herd
 * animal, writes each animal's herd id, and remembers each herd's home for the follow goal.
 */
public final class HerdManager {
    static final int PLAN_INTERVAL = 40;

    private Map<HerdId, HerdHome> homes = new HashMap<>();
    private Map<HerdId, Integer> sizes = new HashMap<>();

    public static HerdManager of(ServerLevel level) {
        return level.getData(HerdAttachments.MANAGER);
    }

    /** A herd's current home in {@code level}, for tests and debug tools. */
    public static Optional<HerdHome> homeOf(ServerLevel level, HerdId herd) {
        return of(level).home(herd);
    }

    Optional<HerdHome> home(HerdId herd) {
        return Optional.ofNullable(homes.get(herd));
    }

    int size(HerdId herd) {
        return sizes.getOrDefault(herd, 0);
    }

    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % PLAN_INTERVAL == 0) {
            of(level).plan(level);
        }
    }

    /** Runs one planning pass now. Public so gametests can force one. */
    public void plan(ServerLevel level) {
        List<HerdAnimal> animals = new ArrayList<>();
        Map<Integer, Animal> byHandle = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Animal animal) || !animal.isAlive() || !HerdBrain.joinsHerds(animal)) {
                continue;
            }
            BuiltInProfiles.forSpecies(EntityFacts.species(animal)).ifPresent(profile -> HerdLookup.preference(animal)
                    .ifPresent(preference -> {
                        Membership membership = HerdLookup.herd(animal)
                                .<Membership>map(Membership.InHerd::new)
                                .orElseGet(Membership.Unassigned::new);
                        animals.add(new HerdAnimal(animal.getId(), profile, preference, EntityFacts.position(animal), membership));
                        byHandle.put(animal.getId(), animal);
                    }));
        }
        McRandomness random = new McRandomness(level.getRandom());
        HerdPlan plan = HerdPlanner.plan(animals, homes, level.getGameTime(), random, new McTerrain(level),
                () -> new HerdId(new UUID(level.getRandom().nextLong(), level.getRandom().nextLong())), HerdPlanSettings.DEFAULT);
        Map<HerdId, Integer> newSizes = new HashMap<>();
        plan.assignments().forEach((handle, herd) -> {
            newSizes.merge(herd, 1, Integer::sum);
            Optional.ofNullable(byHandle.get(handle)).ifPresent(animal -> {
                if (!HerdLookup.herd(animal).equals(Optional.of(herd))) {
                    animal.setData(HerdAttachments.HERD, herd);
                }
            });
        });
        homes = new HashMap<>(plan.homes());
        sizes = newSizes;
        plan.assignments().forEach((handle, herd) -> Optional.ofNullable(byHandle.get(handle)).ifPresent(animal ->
                HerdLookup.preference(animal).ifPresent(p -> HerdDebug.show(animal, p, herd, size(herd),
                        home(herd).map(h -> h.roamer() ? " roaming" : " settled").orElse("")))));
    }
}
