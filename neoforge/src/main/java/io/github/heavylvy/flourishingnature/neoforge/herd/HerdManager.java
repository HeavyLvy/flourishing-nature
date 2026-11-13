package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.BuiltInProfiles;
import io.github.heavylvy.flourishingnature.core.herd.Gallop;
import io.github.heavylvy.flourishingnature.core.herd.HerdAnimal;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlan;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlanSettings;
import io.github.heavylvy.flourishingnature.core.herd.HerdPlanner;
import io.github.heavylvy.flourishingnature.core.herd.Membership;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McTerrain;
import io.github.heavylvy.flourishingnature.neoforge.boundary.ReachableTerrain;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Weather;
import io.github.heavylvy.flourishingnature.neoforge.weather.ShelterExpeditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final ShelterExpeditions expeditions = new ShelterExpeditions();
    private final Map<HerdId, Gallop> gallops = new HashMap<>();
    private final Map<HerdId, Long> restUntil = new HashMap<>();

    /** On average a horse herd breaks into a run once every this many ticks (5 minutes). */
    static final int TICKS_BETWEEN_GALLOPS = 6000;
    /** After a run, a herd rests at least this long before the next. */
    static final int GALLOP_REST = 2400;

    /**
     * A feature that nudges a herd's home after each planning pass, such as moving it toward food
     * or away from danger. Gets the herd's first member as a stand-in for the whole herd.
     */
    @FunctionalInterface
    public interface HomeAdjuster {
        HerdHome adjust(ServerLevel level, HerdId herd, Animal leader, HerdHome home);
    }

    private static final List<HomeAdjuster> ADJUSTERS = new CopyOnWriteArrayList<>();

    /** Adds a home adjuster; they run in the order added, before shelter expeditions. */
    public static void addHomeAdjuster(HomeAdjuster adjuster) {
        ADJUSTERS.add(adjuster);
    }

    private static HerdHome adjust(ServerLevel level, HerdId herd, Animal leader, HerdHome home) {
        HerdHome adjusted = home;
        for (HomeAdjuster adjuster : ADJUSTERS) {
            adjusted = adjuster.adjust(level, herd, leader, adjusted);
        }
        return adjusted;
    }

    /** How far members graze around their herd's home: bigger herds spread out more. */
    public static double grazingRadius(ServerLevel level, HerdId herd) {
        return FollowHerdGoal.spread(of(level).size(herd));
    }

    public static HerdManager of(ServerLevel level) {
        return level.getData(HerdAttachments.MANAGER);
    }

    /** A herd's current home in {@code level}, for tests and debug tools. */
    public static Optional<HerdHome> homeOf(ServerLevel level, HerdId herd) {
        return of(level).home(herd);
    }

    /** Starts a run for {@code herd} now, if its species runs. Used by the debug command and tests. */
    public boolean startGallop(HerdId herd, SpeciesId species, double heading, McRandomness random) {
        if (!Gallop.runs(species)) {
            return false;
        }
        gallops.put(herd, Gallop.start(heading, random));
        return true;
    }

    boolean isGalloping(HerdId herd) {
        return gallops.containsKey(herd);
    }

    /** Moves a running herd's home one stride, or now and then starts a run. */
    private void runGallops(ServerLevel level, HerdId herd, Animal first, McRandomness random) {
        long now = level.getGameTime();
        Optional<HerdHome> home = home(herd);
        if (home.isEmpty()) {
            return;
        }
        Optional<Gallop> running = Optional.ofNullable(gallops.get(herd));
        if (running.isEmpty()) {
            boolean rested = now >= restUntil.getOrDefault(herd, 0L);
            boolean dry = !Weather.precipitating(level, first.blockPosition());
            if (Gallop.runs(EntityFacts.species(first)) && rested && dry
                    && random.nextDouble() < (double) PLAN_INTERVAL / TICKS_BETWEEN_GALLOPS) {
                gallops.put(herd, Gallop.start(random));
            }
            return;
        }
        HerdHome current = home.get();
        Optional<Gallop.Stride> stride = running.get().step(current.anchor(), new ReachableTerrain(first), random);
        if (stride.isPresent()) {
            gallops.put(herd, stride.get().next());
            homes.put(herd, new HerdHome(stride.get().anchor(), current.roamer(), current.nextMove()));
        } else {
            gallops.remove(herd);
            restUntil.put(herd, now + GALLOP_REST);
        }
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
        Map<HerdId, Animal> leader = new HashMap<>();
        plan.assignments().forEach((handle, herd) -> {
            newSizes.merge(herd, 1, Integer::sum);
            Optional.ofNullable(byHandle.get(handle)).ifPresent(a -> leader.putIfAbsent(herd, a));
            Optional.ofNullable(byHandle.get(handle)).ifPresent(animal -> {
                if (!HerdLookup.herd(animal).equals(Optional.of(herd))) {
                    animal.setData(HerdAttachments.HERD, herd);
                }
            });
        });
        homes = new HashMap<>();
        plan.homes().forEach((herd, home) -> homes.put(herd, Optional.ofNullable(leader.get(herd))
                .map(first -> expeditions.update(level, herd, EntityFacts.species(first), first, adjust(level, herd, first, home), random))
                .orElse(home)));
        for (HerdId gone : sizes.keySet()) {
            if (!newSizes.containsKey(gone)) {
                expeditions.forget(gone);
                gallops.remove(gone);
                restUntil.remove(gone);
            }
        }
        leader.forEach((herd, first) -> runGallops(level, herd, first, random));
        sizes = newSizes;
        plan.assignments().forEach((handle, herd) -> Optional.ofNullable(byHandle.get(handle)).ifPresent(animal ->
                HerdLookup.preference(animal).ifPresent(p -> HerdDebug.show(animal, p, herd, size(herd),
                        home(herd).map(h -> h.roamer() ? " roaming" : " settled").orElse("")))));
    }
}
