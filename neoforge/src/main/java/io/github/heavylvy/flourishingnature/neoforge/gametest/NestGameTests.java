package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.nest.NestEgg;
import io.github.heavylvy.flourishingnature.core.nest.NestOwner;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.nest.HenNests;
import io.github.heavylvy.flourishingnature.neoforge.nest.NestBlock;
import io.github.heavylvy.flourishingnature.neoforge.nest.NestBlockEntity;
import io.github.heavylvy.flourishingnature.neoforge.nest.NestConfig;
import io.github.heavylvy.flourishingnature.neoforge.nest.NestRegistry;
import java.util.List;
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
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Hens claim, build, lay in, brood and hatch from nests; players collect eggs. */
public final class NestGameTests {
    private NestGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);

    private static final BlockPos NEST = new BlockPos(14, 1, 14);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLAIMS =
            FUNCTIONS.register("hen_claims_empty_nest", () -> NestGameTests::henClaimsEmptyNest);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILDS_FROM_ITEMS =
            FUNCTIONS.register("hen_builds_nest_from_dropped_material", () -> NestGameTests::henBuildsFromDroppedMaterial);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BUILDS_FROM_GRASS =
            FUNCTIONS.register("hen_builds_nest_from_pulled_grass", () -> NestGameTests::henBuildsFromPulledGrass);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAYS =
            FUNCTIONS.register("hen_lays_in_her_nest", () -> NestGameTests::henLaysInHerNest);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NO_GROUND_EGGS =
            FUNCTIONS.register("chickens_drop_no_eggs_on_the_ground", () -> NestGameTests::noGroundEggs);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BREEDING =
            FUNCTIONS.register("breeding_lays_a_fertile_egg", () -> NestGameTests::breedingLaysFertileEgg);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HATCHES =
            FUNCTIONS.register("fertile_egg_hatches_only_while_sat_on", () -> NestGameTests::hatchesOnlyWhileSatOn);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BROODS =
            FUNCTIONS.register("hen_sits_on_fertile_eggs", () -> NestGameTests::henBroods);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> COLLECTS =
            FUNCTIONS.register("player_collects_eggs", () -> NestGameTests::playerCollectsEggs);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> OWNER_DIES =
            FUNCTIONS.register("dead_hens_nest_is_free", () -> NestGameTests::deadHensNestIsFree);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DECAYS =
            FUNCTIONS.register("abandoned_wild_nest_falls_apart", () -> NestGameTests::abandonedWildNestFallsApart);

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(NestGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("nests"), new TestEnvironmentDefinition.AllOf(List.of()));
        register(event, environment, CLAIMS, 200);
        register(event, environment, BUILDS_FROM_ITEMS, 2400);
        register(event, environment, BUILDS_FROM_GRASS, 2400);
        register(event, environment, LAYS, 400);
        register(event, environment, NO_GROUND_EGGS, 100);
        register(event, environment, BREEDING, 600);
        register(event, environment, HATCHES, 400);
        register(event, environment, BROODS, 600);
        register(event, environment, COLLECTS, 20);
        register(event, environment, OWNER_DIES, 60);
        register(event, environment, DECAYS, 20);
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test,
            int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.getId());
        event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(environment, id("herd_pen"), maxTicks, 0, true)));
    }

    private static Chicken hen(GameTestHelper helper, int x, int z) {
        Chicken hen = helper.spawn(EntityTypes.CHICKEN, x, 1, z);
        FullnessApi.set(hen, Fullness.FULL);
        return hen;
    }

    private static NestBlockEntity nest(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, NestRegistry.NEST.get());
        return helper.getBlockEntity(at, NestBlockEntity.class);
    }

    /** A hen with no nest claims the empty nest 8 blocks away within a second or two. */
    private static void henClaimsEmptyNest(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        Chicken hen = hen(helper, 8, 8);
        helper.succeedWhen(() -> {
            helper.assertTrue(HenNests.nest(hen).filter(mine -> mine == nest).isPresent(), "the hen has not claimed the nest");
            helper.assertTrue(nest.owner().isOwnedBy(hen.getUUID()), "the nest does not name the hen as owner");
        });
    }

    /** Four sticks and two tufts of tall grass on the ground (8 points) become a wild nest. */
    private static void henBuildsFromDroppedMaterial(GameTestHelper helper) {
        TestPens.build(helper);
        Chicken hen = hen(helper, 12, 12);
        int[][] spots = {{9, 12}, {15, 12}, {12, 9}, {12, 15}, {9, 9}, {15, 15}};
        for (int i = 0; i < spots.length; i++) {
            helper.spawnItem(i < 4 ? Items.STICK : Items.TALL_GRASS, new BlockPos(spots[i][0], 1, spots[i][1]));
        }
        helper.succeedWhen(() -> assertBuiltWildNest(helper, hen));
    }

    /** Tall grass growing in the pen (2 points a tuft) is pulled up and becomes a wild nest. */
    private static void henBuildsFromPulledGrass(GameTestHelper helper) {
        TestPens.build(helper);
        Chicken hen = hen(helper, 12, 12);
        for (int[] at : new int[][] {{9, 12}, {15, 12}, {12, 9}, {12, 15}}) {
            helper.setBlock(at[0], 1, at[1], Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
            helper.setBlock(at[0], 2, at[1], Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        }
        helper.succeedWhen(() -> {
            assertBuiltWildNest(helper, hen);
            for (int[] at : new int[][] {{9, 12}, {15, 12}, {12, 9}, {12, 15}}) {
                helper.assertBlockNotPresent(Blocks.TALL_GRASS, new BlockPos(at[0], 1, at[1]));
            }
        });
    }

    private static void assertBuiltWildNest(GameTestHelper helper, Chicken hen) {
        NestBlockEntity nest = HenNests.nest(hen).orElseThrow(() -> helper.assertionException("the hen has no nest yet"));
        helper.assertTrue(nest.isWild(), "the hen's nest is not a wild one she built");
        helper.assertTrue(hen.getMainHandItem().isEmpty(), "the hen still carries material after building");
    }

    /** A hen whose egg is due walks to her nest and lays it there, not on the ground. */
    private static void henLaysInHerNest(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        Chicken hen = hen(helper, 6, 6);
        HenNests.assign(hen, nest);
        HenNests.makeEggDue(hen);
        helper.succeedWhen(() -> {
            helper.assertTrue(nest.clutch().size() == 1, "the nest has " + nest.clutch().size() + " eggs");
            helper.assertBlockProperty(NEST, NestBlock.EGGS, 1);
            helper.assertItemEntityNotPresent(Items.EGG);
        });
    }

    /** Vanilla's egg timer running out drops nothing any more. */
    private static void noGroundEggs(GameTestHelper helper) {
        TestPens.build(helper);
        for (int i = 0; i < 4; i++) {
            Chicken hen = hen(helper, 6 + i * 3, 10);
            hen.eggTime = 1 + i;
        }
        helper.runAtTickTime(60, () -> {
            helper.assertItemEntityNotPresent(Items.EGG);
            helper.succeed();
        });
    }

    /** Two hens in love make no chick; the one with a nest lays a fertile egg in it. */
    private static void breedingLaysFertileEgg(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        Chicken mother = hen(helper, 10, 10);
        Chicken father = hen(helper, 11, 10);
        HenNests.assign(mother, nest);
        mother.setInLoveTime(600);
        father.setInLoveTime(600);
        helper.succeedWhen(() -> {
            helper.assertTrue(nest.clutch().hasFertile(), "no fertile egg in the nest");
            helper.assertBlockProperty(NEST, NestBlock.FERTILE, true);
            long chicks = helper.getEntities(EntityTypes.CHICKEN).stream().filter(Chicken::isBaby).count();
            helper.assertTrue(chicks == 0, chicks + " chicks were born the vanilla way");
        });
    }

    /**
     * A fertile egg just short of hatching stays put while the nest is empty, then hatches once a
     * hen sits on it.
     */
    private static void hatchesOnlyWhileSatOn(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        long almost = NestConfig.settings().incubationTicks() - 100;
        nest.addEgg(new NestEgg.Fertile(almost));
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(nest.clutch().eggs().equals(List.of(new NestEgg.Fertile(almost))),
                    "the egg warmed with no hen on it: " + nest.clutch());
            Chicken sitter = helper.spawn(EntityTypes.CHICKEN, NEST);
            sitter.setNoAi(true);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 150, "hatched before any hen sat on it");
            long chicks = helper.getEntities(EntityTypes.CHICKEN).stream().filter(Chicken::isBaby).count();
            helper.assertTrue(chicks == 1, chicks + " chicks hatched");
            helper.assertTrue(nest.clutch().isEmpty(), "the hatched egg is still in the nest");
        });
    }

    /** A hen with a fertile egg in her nest goes and sits on it. */
    private static void henBroods(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        nest.addEgg(NestEgg.Fertile.fresh());
        Chicken hen = hen(helper, 8, 8);
        HenNests.assign(hen, nest);
        helper.succeedWhen(() -> {
            helper.assertTrue(hen.blockPosition().equals(helper.absolutePos(NEST)), "the hen is not on her nest");
            helper.assertTrue(nest.clutch().eggs().getFirst() instanceof NestEgg.Fertile f && f.warmth() > 0,
                    "the egg has not warmed");
        });
    }

    /** Using a nest takes eggs one at a time; a fertile one comes out as an ordinary egg. */
    private static void playerCollectsEggs(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        nest.addEgg(new NestEgg.Plain());
        nest.addEgg(NestEgg.Fertile.fresh());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.useBlock(NEST, player);
        helper.assertTrue(nest.clutch().size() == 1 && !nest.clutch().hasFertile(), "the newest (fertile) egg was not taken");
        helper.useBlock(NEST, player);
        helper.assertTrue(nest.clutch().isEmpty(), "the nest still has eggs");
        helper.assertTrue(player.getInventory().countItem(Items.EGG) == 2, "the player did not get 2 eggs");
        helper.assertBlockProperty(NEST, NestBlock.EGGS, 0);
        helper.succeed();
    }

    /** When the owner dies her nest is free to claim. */
    private static void deadHensNestIsFree(GameTestHelper helper) {
        TestPens.build(helper);
        NestBlockEntity nest = nest(helper, NEST);
        Chicken hen = hen(helper, 8, 8);
        HenNests.assign(hen, nest);
        hen.kill(helper.getLevel());
        helper.succeedWhen(() -> helper.assertTrue(nest.owner() instanceof NestOwner.Unowned, "the dead hen still owns the nest"));
    }

    /** A wild nest no hen has visited for days crumbles into sticks; a crafted one stays. */
    private static void abandonedWildNestFallsApart(GameTestHelper helper) {
        TestPens.build(helper);
        BlockPos crafted = new BlockPos(8, 1, 8);
        helper.setBlock(NEST, NestRegistry.NEST.get().defaultBlockState().setValue(NestBlock.WILD, true));
        NestBlockEntity wild = helper.getBlockEntity(NEST, NestBlockEntity.class);
        NestBlockEntity kept = nest(helper, crafted);
        long now = helper.getLevel().getGameTime();
        long longAgo = now - NestConfig.settings().wildDecayTicks();
        wild.setLastHenSeen(longAgo);
        kept.setLastHenSeen(longAgo);
        wild.check(helper.getLevel(), 20);
        kept.check(helper.getLevel(), 20);
        helper.assertBlockNotPresent(NestRegistry.NEST.get(), NEST);
        helper.assertItemEntityPresent(Items.STICK, NEST, 2.0);
        helper.assertBlockPresent(NestRegistry.NEST.get(), crafted);
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
