package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks that wellbeing reads the world correctly, in the same walled grass pen. */
public final class WellbeingGameTests {
    private WellbeingGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PASTURE_COW =
            FUNCTIONS.register("pasture_cow_is_content", () -> WellbeingGameTests::pastureCowIsContent);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRAMMED_COWS =
            FUNCTIONS.register("crammed_cows_are_miserable", () -> WellbeingGameTests::crammedCowsAreMiserable);

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(WellbeingGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("wellbeing"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test : List.of(PASTURE_COW, CRAMMED_COWS)) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(environment, id("herd_pen"), 40, 0, true)));
        }
    }

    /** One full cow alone on open grass with plenty of plants, under the sky: sky, food and space are full. */
    private static void pastureCowIsContent(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 7; x <= 21; x += 2) {
            for (int z = 10; z <= 18; z += 4) {
                helper.setBlock(x, 1, z, Blocks.SHORT_GRASS);
            }
        }
        Cow cow = helper.spawn(EntityTypes.COW, 14, 1, 14);
        cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(1));
        FullnessApi.set(cow, Fullness.FULL);
        helper.runAtTickTime(5, () -> {
            WellbeingState state = checkNow(helper, cow);
            helper.assertTrue(state.needs().of(Need.SKY).value() == 1.0, "sky " + state.needs().of(Need.SKY));
            helper.assertTrue(state.needs().of(Need.FOOD).value() == 1.0, "food " + state.needs().of(Need.FOOD));
            helper.assertTrue(state.needs().of(Need.SPACE).value() == 1.0, "space " + state.needs().of(Need.SPACE));
            helper.assertTrue(state.needs().of(Need.SOCIAL).value() == 1.0, "social " + state.needs().of(Need.SOCIAL));
            helper.assertTrue(state.wellbeing().value() >= 50.0, describe(state));
            helper.succeed();
        });
    }

    /** Twelve cows in a dark 3x3 box in a roofed stone barn with no grass: a mob-cramming farm. */
    private static void crammedCowsAreMiserable(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 1; x < TestPens.SIZE - 1; x++) {
            for (int z = 1; z < TestPens.SIZE - 1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, 4, z, Blocks.STONE);
            }
        }
        for (int i = 11; i <= 15; i++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(i, y, 11, Blocks.BARRIER);
                helper.setBlock(i, y, 15, Blocks.BARRIER);
                helper.setBlock(11, y, i, Blocks.BARRIER);
                helper.setBlock(15, y, i, Blocks.BARRIER);
            }
        }
        List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Cow cow = helper.spawn(EntityTypes.COW, 12 + i % 3, 1, 12 + Math.floorDiv(i, 3) % 3);
            cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(3));
            FullnessApi.set(cow, Fullness.EMPTY);
            cows.add(cow);
        }
        helper.runAtTickTime(5, () -> {
            WellbeingState state = checkNow(helper, cows.getFirst());
            helper.assertTrue(state.needs().of(Need.SKY).value() == 0.0, "sky " + state.needs().of(Need.SKY));
            helper.assertTrue(state.needs().of(Need.FOOD).value() == 0.0, "food " + state.needs().of(Need.FOOD));
            helper.assertTrue(state.needs().of(Need.SPACE).value() == 0.0, "space " + state.needs().of(Need.SPACE));
            helper.assertTrue(state.wellbeing().value() < 25.0, describe(state));
            helper.succeed();
        });
    }

    private static WellbeingState checkNow(GameTestHelper helper, Cow cow) {
        WellbeingEvents.check(cow, helper.getLevel().getGameTime());
        return cow.getExistingData(WellbeingAttachments.STATE)
                .orElseThrow(() -> helper.assertionException("cow has no wellbeing after a check"));
    }

    private static String describe(WellbeingState state) {
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT, "wellbeing %.1f:", state.wellbeing().value()));
        for (Need need : Need.values()) {
            text.append(String.format(Locale.ROOT, " %s=%.2f", need.name().toLowerCase(Locale.ROOT), state.needs().of(need).value()));
        }
        return text.toString();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
