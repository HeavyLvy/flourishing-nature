package io.github.heavylvy.flourishingnature.neoforge.predator;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.predator.Diet;
import io.github.heavylvy.flourishingnature.core.predator.Dispersal;
import io.github.heavylvy.flourishingnature.core.predator.PackMemory;
import io.github.heavylvy.flourishingnature.core.predator.PackRules;
import io.github.heavylvy.flourishingnature.core.predator.PackSettings;
import io.github.heavylvy.flourishingnature.core.predator.PreyChoice;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Hunting;
import io.github.heavylvy.flourishingnature.neoforge.boundary.McRandomness;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * The pack pass: every {@link #INTERVAL} ticks (between the herd planner's passes) each wild wolf
 * pack, which is a herd of wild wolves, looks at its own hunger:
 *
 * <ul>
 *   <li>hungry: drifts its home toward the weakest prey it can smell within {@code scentRadius}
 *       (done as a herd-planner home adjuster, see {@link #driftTowardPrey});
 *   <li>full: two ready adults have a pup, if the pack has room;
 *   <li>starving and days without a kill: the pack breaks up and each wolf travels off alone.
 * </ul>
 *
 * <p>Public methods double as test hooks so gametests don't wait days.
 */
public final class WolfPacks {
    private WolfPacks() {}

    static final int INTERVAL = 40;
    static final int OFFSET = 20;

    /** Whether this animal is a wolf travelling away from its broken pack. */
    public static boolean isDispersing(Animal animal) {
        return animal instanceof Wolf && animal.getExistingData(PredatorAttachments.DISPERSAL).isPresent();
    }

    public static PackMemory memory(Animal wolf) {
        return wolf.getData(PredatorAttachments.MEMORY);
    }

    /** Test hook: pretend the wolf last ate {@code ticksAgo} ticks ago. */
    public static void setLastMeal(Animal wolf, long ticksAgo) {
        long now = wolf.level().getGameTime();
        wolf.setData(PredatorAttachments.MEMORY, new PackMemory(now - ticksAgo, memory(wolf).lastBred()));
    }

    /** Test hook: pretend the wolf had a pup {@code ticksAgo} ticks ago. */
    public static void setLastBred(Animal wolf, long ticksAgo) {
        long now = wolf.level().getGameTime();
        wolf.setData(PredatorAttachments.MEMORY, memory(wolf).bred(now - ticksAgo));
    }

    static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL == OFFSET) {
            runPass(level);
        }
    }

    /** Runs one pack pass now. Public so gametests can force one. */
    public static void runPass(ServerLevel level) {
        long now = level.getGameTime();
        PackSettings s = PredatorConfig.settings();
        Map<HerdId, List<Wolf>> packs = new LinkedHashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Wolf wolf) || !wolf.isAlive() || wolf.isTame()) {
                continue;
            }
            Optional<Dispersal> travelling = wolf.getExistingData(PredatorAttachments.DISPERSAL);
            if (travelling.isPresent()) {
                if (!travelling.get().active(now, EntityFacts.position(wolf))) {
                    wolf.removeData(PredatorAttachments.DISPERSAL);
                }
                continue;
            }
            wolf.getExistingData(HerdAttachments.HERD)
                    .ifPresent(herd -> packs.computeIfAbsent(herd, id -> new ArrayList<>()).add(wolf));
        }
        packs.forEach((herd, members) -> runPack(level, members, now, s));
    }

    private static void runPack(ServerLevel level, List<Wolf> members, long now, PackSettings s) {
        List<Wolf> adults = members.stream().filter(w -> !w.isBaby()).toList();
        if (adults.isEmpty()) {
            return;
        }
        List<Fullness> fullness = adults.stream().map(FullnessApi::fullness).toList();
        HungerBand packHunger = PackRules.packFullness(fullness).band();
        long lastMeal = adults.stream().mapToLong(w -> memory(w).lastMeal()).max().orElse(now);
        List<HungerBand> bands = fullness.stream().map(Fullness::band).toList();
        if (PredatorConfig.packSplitting() && PackRules.shouldSplit(bands, lastMeal, now, s)) {
            split(level, members, now, s);
            return;
        }
        if (PredatorConfig.packBreeding()) {
            breed(level, members, adults, packHunger, now, s);
        }
    }

    /**
     * Home adjuster for the herd planner: a hungry wild pack moves its home a step toward the
     * weakest prey it can smell. Runs on every planning pass, for every herd.
     */
    static HerdHome driftTowardPrey(ServerLevel level, HerdId herd, Animal leader, HerdHome home) {
        if (!(leader instanceof Wolf first) || first.isTame() || !PredatorConfig.hungerGatesHunting()) {
            return home;
        }
        PackSettings s = PredatorConfig.settings();
        List<Fullness> fullness = level.getEntitiesOfClass(Wolf.class, first.getBoundingBox().inflate(s.scentRadius()),
                        w -> w.isAlive() && !w.isBaby() && w.getExistingData(HerdAttachments.HERD).equals(Optional.of(herd)))
                .stream().map(FullnessApi::fullness).toList();
        if (fullness.isEmpty()) {
            return home;
        }
        Diet diet = Diet.of(PackRules.packFullness(fullness).band());
        if (!diet.hunts()) {
            return home;
        }
        Position anchor = home.anchor();
        return PreyChoice.pick(anchor, WolfHunting.asPrey(WolfHunting.preyNear(level, anchor, s.scentRadius(), diet)), s.scentRadius())
                .map(prey -> new HerdHome(PackRules.driftToward(anchor, prey.position(), s), home.roamer(), home.nextMove()))
                .orElse(home);
    }

    private static void breed(ServerLevel level, List<Wolf> members, List<Wolf> adults, HungerBand packHunger, long now, PackSettings s) {
        List<Wolf> ready = adults.stream()
                .filter(w -> w.getAge() == 0 && !w.isInWater()
                        && ProductionRules.canSelfBreed(mood(w), FullnessApi.band(w))
                        && PackRules.pastBreedCooldown(memory(w), now, s))
                .toList();
        if (!PackRules.mayBreed(ready.size(), members.size(), packHunger, s)) {
            return;
        }
        Wolf mother = ready.get(0);
        Wolf father = ready.get(1);
        // Vanilla only lets tame wolves fall in love, so the pack's pup is born directly. This
        // still fires BabyEntitySpawnEvent, so the pup joins the pack and inherits a preference.
        mother.spawnChildFromBreeding(level, father);
        mother.setData(PredatorAttachments.MEMORY, memory(mother).bred(now));
        father.setData(PredatorAttachments.MEMORY, memory(father).bred(now));
    }

    /** A wolf never checked for wellbeing yet counts as content, as in production. */
    private static Mood mood(Wolf wolf) {
        return wolf.getExistingData(WellbeingAttachments.STATE).map(st -> st.wellbeing().mood()).orElse(Mood.CONTENT);
    }

    private static void split(ServerLevel level, List<Wolf> members, long now, PackSettings s) {
        List<Position> points = members.stream().map(EntityFacts::position).toList();
        List<Position> targets = PackRules.dispersalTargets(
                Position.centroid(points), members.size(), new McRandomness(level.getRandom()), s);
        for (int i = 0; i < members.size(); i++) {
            Wolf wolf = members.get(i);
            wolf.removeData(HerdAttachments.HERD);
            wolf.setData(PredatorAttachments.DISPERSAL, new Dispersal(targets.get(i), now + s.dispersalTime()));
            // A fresh start: in new territory the wolf gets a full spell to find food.
            wolf.setData(PredatorAttachments.MEMORY, memory(wolf).fed(now));
        }
    }

    /**
     * A wild wolf made a kill: every pack mate near it eats its fill, and stops chasing other prey.
     * Returns how many wolves ate.
     */
    static int shareKill(Wolf killer, Position at, PackSettings s) {
        long now = killer.level().getGameTime();
        Optional<HerdId> herd = killer.getExistingData(HerdAttachments.HERD);
        List<Wolf> eaters = new ArrayList<>(List.of(killer));
        herd.ifPresent(id -> eaters.addAll(killer.level().getEntitiesOfClass(Wolf.class,
                killer.getBoundingBox().inflate(s.shareRadius()),
                w -> w.getId() != killer.getId() && w.isAlive() && !w.isTame() && w.getExistingData(HerdAttachments.HERD).equals(herd)
                        && EntityFacts.position(w).distanceTo(at) <= s.shareRadius())));
        for (Wolf wolf : eaters) {
            FullnessApi.set(wolf, Fullness.FULL);
            wolf.setData(PredatorAttachments.MEMORY, memory(wolf).fed(now));
            Hunting.target(wolf).filter(WolfPacks::isFood).ifPresent(prey -> Hunting.clearTarget(wolf));
        }
        return eaters.size();
    }

    private static boolean isFood(LivingEntity target) {
        return target instanceof Animal;
    }
}
