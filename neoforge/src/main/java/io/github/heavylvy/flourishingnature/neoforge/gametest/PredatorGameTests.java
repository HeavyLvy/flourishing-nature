package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.fullness.FullnessRates;
import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Hunting;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdManager;
import io.github.heavylvy.flourishingnature.neoforge.predator.WolfPacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks for wild wolf packs: hunger-driven hunting, shared kills, pups and break-ups. */
public final class PredatorGameTests {
    private PredatorGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);

    private static final Identifier PEN = id("herd_pen");
    private static final double HUNGRY = 0.3;

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FED_WOLF_IGNORES_SHEEP =
            FUNCTIONS.register("fed_wolf_ignores_sheep", () -> PredatorGameTests::fedWolfIgnoresSheep);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HUNGRY_WOLF_TAKES_THE_LAMB =
            FUNCTIONS.register("hungry_wolf_takes_the_lamb", () -> PredatorGameTests::hungryWolfTakesTheLamb);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> KILL_FEEDS_THE_PACK =
            FUNCTIONS.register("kill_feeds_the_pack", () -> PredatorGameTests::killFeedsThePack);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HUNGRY_PACK_DRIFTS_TO_PREY =
            FUNCTIONS.register("hungry_pack_drifts_to_prey", () -> PredatorGameTests::hungryPackDriftsToPrey);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FULL_PACK_HAS_A_PUP =
            FUNCTIONS.register("full_pack_has_a_pup", () -> PredatorGameTests::fullPackHasAPup);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARVING_PACK_BREAKS_UP =
            FUNCTIONS.register("starving_pack_breaks_up", () -> PredatorGameTests::starvingPackBreaksUp);

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(PredatorGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("predators"), new TestEnvironmentDefinition.AllOf(List.of()));
        registerTest(event, environment, FED_WOLF_IGNORES_SHEEP, 300);
        registerTest(event, environment, HUNGRY_WOLF_TAKES_THE_LAMB, 200);
        registerTest(event, environment, KILL_FEEDS_THE_PACK, 40);
        registerTest(event, environment, HUNGRY_PACK_DRIFTS_TO_PREY, 400);
        registerTest(event, environment, FULL_PACK_HAS_A_PUP, 100);
        registerTest(event, environment, STARVING_PACK_BREAKS_UP, 400);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
            int maxTicks) {
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, function.getId());
        event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(environment, PEN, maxTicks, 0, true)));
    }

    /** A fed wild wolf next to a sheep leaves it alone (vanilla wolves would attack it). */
    private static void fedWolfIgnoresSheep(GameTestHelper helper) {
        TestPens.build(helper);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 14, 1, 14);
        FullnessApi.set(wolf, new Fullness(0.6));
        Sheep sheep = frozenSheep(helper, 17, 14, false);
        helper.onEachTick(() -> helper.assertTrue(Hunting.target(wolf).isEmpty() && sheep.getHealth() == sheep.getMaxHealth(),
                "a fed wolf went after the sheep"));
        helper.runAtTickTime(280, helper::succeed);
    }

    /** A hungry wolf goes for the lamb 10 blocks away rather than the healthy sheep 4 blocks away. */
    private static void hungryWolfTakesTheLamb(GameTestHelper helper) {
        TestPens.build(helper);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 8, 1, 14);
        FullnessApi.set(wolf, new Fullness(HUNGRY));
        Sheep adult = frozenSheep(helper, 12, 14, false);
        Sheep lamb = frozenSheep(helper, 8, 4, true);
        AtomicInteger firstTarget = new AtomicInteger(-1);
        helper.onEachTick(() -> Hunting.target(wolf).ifPresent(t -> firstTarget.compareAndSet(-1, t.getId())));
        helper.succeedWhen(() -> {
            helper.assertTrue(firstTarget.get() != -1, "the wolf is not hunting");
            helper.assertTrue(firstTarget.get() == lamb.getId(),
                    firstTarget.get() == adult.getId() ? "the wolf went for the healthy adult first" : "the wolf hunted something else");
        });
    }

    /** A wolf kills a sheep: the whole pack nearby is full, and there is no mutton or wool on the ground. */
    private static void killFeedsThePack(GameTestHelper helper) {
        TestPens.build(helper);
        HerdId pack = new HerdId(UUID.randomUUID());
        List<Wolf> wolves = new ArrayList<>();
        for (int x = 10; x <= 14; x += 2) {
            Wolf wolf = helper.spawn(EntityTypes.WOLF, x, 1, 14);
            wolf.setData(HerdAttachments.HERD, pack);
            FullnessApi.set(wolf, new Fullness(0.1));
            wolves.add(wolf);
        }
        Sheep sheep = frozenSheep(helper, 12, 16, false);
        sheep.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(wolves.getFirst()), 100.0F);
        helper.assertTrue(!sheep.isAlive(), "the sheep survived");
        helper.runAtTickTime(5, () -> {
            for (Wolf wolf : wolves) {
                helper.assertTrue(FullnessApi.band(wolf) == HungerBand.FULL, "a pack mate did not eat");
            }
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, penBox(helper));
            helper.assertTrue(drops.isEmpty(), drops.size() + " items dropped from the kill");
            helper.succeed();
        });
    }

    /** A hungry pack at one end of the pen moves its home toward a sheep 20 blocks away. */
    private static void hungryPackDriftsToPrey(GameTestHelper helper) {
        TestPens.build(helper);
        HerdId pack = new HerdId(UUID.randomUUID());
        for (int z = 12; z <= 16; z += 2) {
            Wolf wolf = helper.spawn(EntityTypes.WOLF, 3, 1, z);
            wolf.setData(HerdAttachments.HERD, pack);
            wolf.setData(HerdAttachments.PREFERENCE, new HerdPreference(4));
            FullnessApi.set(wolf, new Fullness(HUNGRY));
        }
        Sheep sheep = frozenSheep(helper, 24, 14, false);
        sheep.setPermanentlyInvulnerable(true);
        helper.succeedWhen(() -> {
            HerdHome home = HerdManager.homeOf(helper.getLevel(), pack)
                    .orElseThrow(() -> helper.assertionException("the pack has no home yet"));
            double x = helper.relativeVec(new Vec3(home.anchor().x(), home.anchor().y(), home.anchor().z())).x;
            helper.assertTrue(x >= 14.0, "pack home still at x=" + String.format(Locale.ROOT, "%.1f", x));
        });
    }

    /** Two full adults in a pack have a pup that joins the pack; the cooldown stops a second one. */
    private static void fullPackHasAPup(GameTestHelper helper) {
        TestPens.build(helper);
        HerdId pack = new HerdId(UUID.randomUUID());
        List<Wolf> parents = new ArrayList<>();
        for (int x = 13; x <= 15; x += 2) {
            Wolf wolf = helper.spawn(EntityTypes.WOLF, x, 1, 14);
            wolf.setData(HerdAttachments.HERD, pack);
            wolf.setData(HerdAttachments.PREFERENCE, new HerdPreference(4));
            FullnessApi.set(wolf, Fullness.FULL);
            parents.add(wolf);
        }
        WolfPacks.runPass(helper.getLevel());
        helper.assertTrue(pups(helper).size() == 1, pups(helper).size() + " pups after one pass");
        helper.assertTrue(pups(helper).getFirst().getExistingData(HerdAttachments.HERD).equals(Optional.of(pack)),
                "the pup is not in the pack");
        for (Wolf parent : parents) {
            parent.setAge(0);
            FullnessApi.set(parent, Fullness.FULL);
        }
        WolfPacks.runPass(helper.getLevel());
        helper.assertTrue(pups(helper).size() == 1, "the parents bred again within the cooldown");
        for (Wolf parent : parents) {
            WolfPacks.setLastBred(parent, 3 * FullnessRates.DAY);
        }
        WolfPacks.runPass(helper.getLevel());
        helper.assertTrue(pups(helper).size() == 2, "no second pup after the cooldown");
        helper.succeed();
    }

    /**
     * A pack starving for five days without a kill breaks up: the wolves leave the herd and head off
     * in different directions. Nobody dies of hunger.
     */
    private static void starvingPackBreaksUp(GameTestHelper helper) {
        TestPens.build(helper);
        HerdId pack = new HerdId(UUID.randomUUID());
        List<Wolf> wolves = new ArrayList<>();
        for (int x = 12; x <= 16; x += 2) {
            Wolf wolf = helper.spawn(EntityTypes.WOLF, x, 1, 14);
            wolf.setData(HerdAttachments.HERD, pack);
            wolf.setData(HerdAttachments.PREFERENCE, new HerdPreference(4));
            FullnessApi.set(wolf, Fullness.EMPTY);
            WolfPacks.setLastMeal(wolf, 5 * FullnessRates.DAY);
            wolves.add(wolf);
        }
        WolfPacks.runPass(helper.getLevel());
        for (Wolf wolf : wolves) {
            helper.assertTrue(wolf.getExistingData(HerdAttachments.HERD).isEmpty(), "a wolf is still in the pack");
            helper.assertTrue(WolfPacks.isDispersing(wolf), "a wolf is not travelling away");
        }
        helper.succeedWhen(() -> {
            for (Wolf wolf : wolves) {
                helper.assertTrue(wolf.isAlive() && wolf.getHealth() == wolf.getMaxHealth(), "a starving wolf got hurt");
                helper.assertTrue(wolf.getExistingData(HerdAttachments.HERD).isEmpty(), "a wolf rejoined a pack");
            }
            double spread = 0;
            for (Wolf a : wolves) {
                for (Wolf b : wolves) {
                    spread = Math.max(spread, a.distanceTo(b));
                }
            }
            helper.assertTrue(spread > 14.0, "wolves only " + String.format(Locale.ROOT, "%.1f", spread) + " blocks apart");
        });
    }

    private static List<Wolf> pups(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Wolf.class, penBox(helper), Wolf::isBaby);
    }

    private static AABB penBox(GameTestHelper helper) {
        return new AABB(helper.absoluteVec(Vec3.ZERO), helper.absoluteVec(new Vec3(TestPens.SIZE, 6, TestPens.SIZE)));
    }

    private static Sheep frozenSheep(GameTestHelper helper, int x, int z, boolean baby) {
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, x, 1, z);
        sheep.setNoAi(true);
        if (baby) {
            sheep.setBaby(true);
        }
        return sheep;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
