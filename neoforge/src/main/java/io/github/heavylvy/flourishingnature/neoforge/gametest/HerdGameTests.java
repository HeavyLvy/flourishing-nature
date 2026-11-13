package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks that herding actually moves animals. The pen is a 28x28 grass floor with walls. */
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
        List<Cow> cows = List.of(
                cowAt(helper, 7, 7), cowAt(helper, 19, 7), cowAt(helper, 7, 19), cowAt(helper, 19, 19));
        helper.succeedWhen(() -> {
            double spread = maxPairwiseDistance(cows);
            helper.assertTrue(spread < 9.0, "cows still " + String.format(Locale.ROOT, "%.1f", spread) + " blocks apart");
        });
    }

    /** A cow spawned in the world rolls a preference within the cow profile's possible values. */
    private static void spawnedCowGetsPreference(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 14, 1, 14);
        Optional<HerdPreference> preference = cow.getExistingData(HerdAttachments.PREFERENCE);
        helper.assertTrue(preference.isPresent(), "spawned cow has no herd preference");
        int value = preference.map(HerdPreference::value).orElse(0);
        helper.assertTrue(value >= 1 && value <= 14, "cow preference " + value + " outside 1..14");
        helper.succeed();
    }

    /** Pigs have no affinity for cows, so a pig that wants company does not walk to a cow herd. */
    private static void pigsIgnoreCows(GameTestHelper helper) {
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

    private static Cow cowAt(GameTestHelper helper, int x, int z) {
        Cow cow = helper.spawn(EntityTypes.COW, x, 1, z);
        cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(5));
        return cow;
    }

    private static double maxPairwiseDistance(List<Cow> cows) {
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
