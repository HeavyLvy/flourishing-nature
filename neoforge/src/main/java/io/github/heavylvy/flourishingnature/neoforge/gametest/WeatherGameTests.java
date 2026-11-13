package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Weather;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.weather.SeekShelterGoal;
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
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(rain, id("herd_pen"), 400, 0, true)));
        }
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
        BlockPos from = helper.absolutePos(new BlockPos(0, -1, 0));
        BlockPos to = helper.absolutePos(new BlockPos(TestPens.SIZE - 1, 6, TestPens.SIZE - 1));
        String command = String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s",
                from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), biome);
        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), command);
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
