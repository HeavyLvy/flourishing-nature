package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Weather;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.weather.SeekShelterGoal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Rain tests, in their own rainy test environment so other tests stay dry. */
public final class WeatherGameTests {
    private WeatherGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COW_SHELTERS =
            FUNCTIONS.register("cow_shelters_from_rain", () -> WeatherGameTests::cowSheltersFromRain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DESERT_COW =
            FUNCTIONS.register("desert_cow_ignores_rain", () -> WeatherGameTests::desertCowIgnoresRain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HERD_EXPLORES =
            FUNCTIONS.register("herd_explores_for_shelter", () -> WeatherGameTests::herdExploresForShelter);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WOLF_IGNORES_RAIN =
            FUNCTIONS.register("wolf_ignores_rain", () -> WeatherGameTests::wolfIgnoresRain);

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(WeatherGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> rain = event.registerEnvironment(
                id("rain"), new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.RAIN));
        for (DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test : List.of(COW_SHELTERS, DESERT_COW, WOLF_IGNORES_RAIN)) {
            register(event, rain, test, 400);
        }
        register(event, rain, HERD_EXPLORES, 2400);
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test,
            int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.getId());
        event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(environment, id("herd_pen"), maxTicks, 0, true)));
    }

    /**
     * In the rain, a cow out in the open walks under a small leafy roof 10 blocks away. The test
     * world is a desert (no rain), so the pen is turned into plains first.
     */
    private static void cowSheltersFromRain(GameTestHelper helper) {
        TestPens.build(helper);
        setBiome(helper, "minecraft:plains");
        for (int x = 18; x <= 20; x++) {
            for (int z = 13; z <= 15; z++) {
                helper.setBlock(x, 3, z, Blocks.OAK_LEAVES);
            }
        }
        Cow cow = helper.spawn(EntityTypes.COW, 9, 1, 14);
        cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(1));
        helper.succeedWhen(() -> helper.assertTrue(Weather.covered(cow), "cow is still out in the rain"));
    }

    /**
     * A small herd caught in the rain with no cover within reach explores together and finds the
     * grove at the far corner of the pen, about 26 blocks away.
     */
    private static void herdExploresForShelter(GameTestHelper helper) {
        TestPens.build(helper);
        setBiome(helper, "minecraft:plains");
        for (int x = 21; x <= 25; x++) {
            for (int z = 21; z <= 25; z++) {
                helper.setBlock(x, 3, z, Blocks.OAK_LEAVES);
            }
        }
        List<Cow> cows = new ArrayList<>();
        for (int[] at : new int[][] {{3, 3}, {4, 3}, {3, 4}}) {
            Cow cow = helper.spawn(EntityTypes.COW, at[0], 1, at[1]);
            cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(3));
            cows.add(cow);
        }
        helper.succeedWhen(() -> {
            long sheltered = cows.stream().filter(Weather::covered).count();
            helper.assertTrue(sheltered >= 2, sheltered + " of 3 cows found cover");
        });
    }

    /** In a desert it never actually rains, so a cow there doesn't go looking for cover. */
    private static void desertCowIgnoresRain(GameTestHelper helper) {
        TestPens.build(helper);
        setBiome(helper, "minecraft:desert");
        Cow cow = helper.spawn(EntityTypes.COW, 9, 1, 14);
        cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(1));
        helper.runAtTickTime(200, () -> {
            boolean sheltering = cow.goalSelector.getAvailableGoals().stream()
                    .anyMatch(wrapped -> wrapped.getGoal() instanceof SeekShelterGoal && wrapped.isRunning());
            helper.assertTrue(!sheltering, "the desert cow is hiding from rain that isn't falling");
            helper.succeed();
        });
    }

    private static void setBiome(GameTestHelper helper, String biome) {
        setBiome(helper, biome, TestPens.SIZE);
    }

    /** Runs /fillbiome over the pen in 32x32 tiles, staying under the block-modification limit. */
    private static void setBiome(GameTestHelper helper, String biome, int size) {
        for (int x0 = 0; x0 < size; x0 += 32) {
            for (int z0 = 0; z0 < size; z0 += 32) {
                BlockPos from = helper.absolutePos(new BlockPos(x0, -1, z0));
                BlockPos to = helper.absolutePos(new BlockPos(Math.min(size, x0 + 32) - 1, 6, Math.min(size, z0 + 32) - 1));
                String command = String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s",
                        from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), biome);
                helper.getLevel().getServer().getCommands().performPrefixedCommand(
                        helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), command);
            }
        }
    }

    /** Wolves don't mind the weather: they never get the shelter goal. */
    private static void wolfIgnoresRain(GameTestHelper helper) {
        TestPens.build(helper);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 9, 1, 14);
        boolean seeksShelter = wolf.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof SeekShelterGoal);
        helper.assertTrue(!seeksShelter, "the wolf would hide from the rain");
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
