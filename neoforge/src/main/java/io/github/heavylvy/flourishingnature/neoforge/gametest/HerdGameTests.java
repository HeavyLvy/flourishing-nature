package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks that herding actually moves animals, in a walled 28x28 grass pen. */
public final class HerdGameTests {
    private HerdGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);

    private static final Identifier PEN = id("herd_pen");
    private static final int MAX_TICKS = 1200;

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCATTERED_COWS_GATHER =
            FUNCTIONS.register("scattered_cows_gather", () -> HerdGameTests::scatteredCowsGather);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPAWNED_COW_GETS_PREFERENCE =
            FUNCTIONS.register("spawned_cow_gets_preference", () -> HerdGameTests::spawnedCowGetsPreference);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PIGS_IGNORE_COWS =
            FUNCTIONS.register("pigs_ignore_cows", () -> HerdGameTests::pigsIgnoreCows);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HERD_PANICS_TOGETHER =
            FUNCTIONS.register("herd_panics_together", () -> HerdGameTests::herdPanicsTogether);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHEEP_FLEE_WOLF =
            FUNCTIONS.register("sheep_flee_wolf", () -> HerdGameTests::sheepFleeWolf);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHICKEN_FLEES_FOX =
            FUNCTIONS.register("chicken_flees_fox", () -> HerdGameTests::chickenFleesFox);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WOLVES_FORM_A_PACK =
            FUNCTIONS.register("wolves_form_a_pack", () -> HerdGameTests::wolvesFormAPack);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WOLF_CATCHES_FLEEING_SHEEP =
            FUNCTIONS.register("wolf_catches_fleeing_sheep", () -> HerdGameTests::wolfCatchesFleeingSheep);

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(HerdGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        registerTest(event, environment, SCATTERED_COWS_GATHER, MAX_TICKS);
        registerTest(event, environment, SPAWNED_COW_GETS_PREFERENCE, 20);
        registerTest(event, environment, PIGS_IGNORE_COWS, 400);
        registerTest(event, environment, HERD_PANICS_TOGETHER, 100);
        registerTest(event, environment, SHEEP_FLEE_WOLF, 200);
        registerTest(event, environment, CHICKEN_FLEES_FOX, 200);
        registerTest(event, environment, WOLVES_FORM_A_PACK, MAX_TICKS);
        registerTest(event, environment, WOLF_CATCHES_FLEEING_SHEEP, 600);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
            int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, function.getId());
        event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(environment, PEN, maxTicks, 0, true)));
    }

    /** Four cows that want a herd of 5, placed 12 blocks apart, should end up as one group. */
    private static void scatteredCowsGather(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = List.of(
                cowAt(helper, 7, 7), cowAt(helper, 19, 7), cowAt(helper, 7, 19), cowAt(helper, 19, 19));
        helper.succeedWhen(() -> {
            double spread = maxPairwiseDistance(cows);
            helper.assertTrue(spread < 9.0, "cows still " + String.format(Locale.ROOT, "%.1f", spread) + " blocks apart");
        });
    }

    /** A cow spawned in the world rolls a preference within the cow profile's possible values. */
    private static void spawnedCowGetsPreference(GameTestHelper helper) {
        TestPens.build(helper);
        Cow cow = helper.spawn(EntityTypes.COW, 14, 1, 14);
        Optional<HerdPreference> preference = cow.getExistingData(HerdAttachments.PREFERENCE);
        helper.assertTrue(preference.isPresent(), "spawned cow has no herd preference");
        int value = preference.map(HerdPreference::value).orElse(0);
        helper.assertTrue(value >= 1 && value <= 14, "cow preference " + value + " outside 1..14");
        helper.succeed();
    }

    /** Pigs have no affinity for cows, so a pig that wants company does not walk to a cow herd. */
    private static void pigsIgnoreCows(GameTestHelper helper) {
        TestPens.build(helper);
        cowAt(helper, 20, 14);
        cowAt(helper, 21, 15);
        cowAt(helper, 20, 16);
        Pig pig = helper.spawn(EntityTypes.PIG, 8, 1, 14);
        pig.setData(HerdAttachments.PREFERENCE, new HerdPreference(4));
        helper.runAtTickTime(380, () -> {
            double distance = pig.position().distanceTo(helper.absoluteVec(new Vec3(20.5, 1, 15.5)));
            helper.assertTrue(distance > 4.0, "pig joined the cow herd (" + distance + " blocks away)");
            helper.succeed();
        });
    }

    /**
     * Hitting one cow sends its herd mates running, each its own way, while a nearby pig (no
     * affinity for cows) stays calm.
     */
    private static void herdPanicsTogether(GameTestHelper helper) {
        TestPens.build(helper);
        Cow victim = cowAt(helper, 14, 14);
        List<Cow> mates = List.of(cowAt(helper, 16, 14), cowAt(helper, 14, 16), cowAt(helper, 12, 13));
        Pig pig = helper.spawn(EntityTypes.PIG, 15, 1, 12);
        pig.setData(HerdAttachments.PREFERENCE, new HerdPreference(3));
        List<Vec3> start = mates.stream().map(Cow::position).toList();
        victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(pig), 1.0F);
        for (Cow mate : mates) {
            helper.assertTrue(mate.getExistingData(HerdAttachments.ALARM).isPresent(), "a herd mate did not get alarmed");
        }
        helper.assertTrue(pig.getExistingData(HerdAttachments.ALARM).isEmpty(), "the pig panicked with the cows");
        helper.runAtTickTime(60, () -> {
            for (int i = 0; i < mates.size(); i++) {
                double moved = mates.get(i).position().distanceTo(start.get(i));
                helper.assertTrue(moved > 3.0, "a herd mate only moved " + String.format(Locale.ROOT, "%.1f", moved) + " blocks");
            }
            helper.succeed();
        });
    }

    /** A sheep runs from a wild wolf before it gets bitten. The wolf is frozen so it can't chase. */
    private static void sheepFleeWolf(GameTestHelper helper) {
        TestPens.build(helper);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 14, 1, 14);
        wolf.setNoAi(true);
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 8, 1, 14);
        helper.succeedWhen(() -> helper.assertTrue(
                sheep.distanceTo(wolf) > 9.0, "sheep still " + String.format(Locale.ROOT, "%.1f", sheep.distanceTo(wolf)) + " blocks from the wolf"));
    }

    /** A chicken runs from a fox. */
    private static void chickenFleesFox(GameTestHelper helper) {
        TestPens.build(helper);
        Fox fox = helper.spawn(EntityTypes.FOX, 14, 1, 14);
        fox.setNoAi(true);
        Chicken chicken = helper.spawn(EntityTypes.CHICKEN, 9, 1, 14);
        helper.succeedWhen(() -> helper.assertTrue(
                chicken.distanceTo(fox) > 8.0, "chicken still " + String.format(Locale.ROOT, "%.1f", chicken.distanceTo(fox)) + " blocks from the fox"));
    }

    /** Four wild wolves that want a pack of 4, placed 12 blocks apart, end up together. */
    private static void wolvesFormAPack(GameTestHelper helper) {
        TestPens.build(helper);
        List<Wolf> wolves = new ArrayList<>();
        for (int[] at : new int[][] {{7, 7}, {19, 7}, {7, 19}, {19, 19}}) {
            Wolf wolf = helper.spawn(EntityTypes.WOLF, at[0], 1, at[1]);
            wolf.setData(HerdAttachments.PREFERENCE, new HerdPreference(4));
            wolves.add(wolf);
        }
        helper.succeedWhen(() -> {
            double spread = maxPairwiseDistance(wolves);
            helper.assertTrue(spread < 9.0, "wolves still " + String.format(Locale.ROOT, "%.1f", spread) + " blocks apart");
        });
    }

    /** A sheep flees a hunting wolf but runs slightly slower, so the wolf catches it. */
    private static void wolfCatchesFleeingSheep(GameTestHelper helper) {
        TestPens.build(helper);
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 14, 1, 10);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 14, 1, 18);
        wolf.setTarget(sheep);
        helper.succeedWhen(() -> helper.assertTrue(sheep.getHealth() < sheep.getMaxHealth() || !sheep.isAlive(),
                "the wolf never caught the sheep"));
    }

    private static Cow cowAt(GameTestHelper helper, int x, int z) {
        Cow cow = helper.spawn(EntityTypes.COW, x, 1, z);
        cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(5));
        return cow;
    }

    private static double maxPairwiseDistance(List<? extends Entity> cows) {
        double max = 0;
        for (int i = 0; i < cows.size(); i++) {
            for (int j = i + 1; j < cows.size(); j++) {
                max = Math.max(max, cows.get(i).position().distanceTo(cows.get(j).position()));
            }
        }
        return max;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
