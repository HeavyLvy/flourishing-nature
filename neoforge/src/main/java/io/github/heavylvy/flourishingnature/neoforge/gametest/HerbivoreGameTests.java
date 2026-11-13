package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.herbivore.SelfBreeding;
import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScore;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScores;
import io.github.heavylvy.flourishingnature.core.wellbeing.RollingMemory;
import io.github.heavylvy.flourishingnature.core.wellbeing.Wellbeing;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.DangerMemories;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.HerbivoreAttachments;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.SelfBreedingCheck;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdAttachments;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdManager;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingEvents;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks that herbivores eat real plants, breed on their own and move home for food and safety. */
public final class HerbivoreGameTests {
    private HerbivoreGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);
    private static final List<Registration> TESTS = new ArrayList<>();
    private static final Fullness HUNGRY = new Fullness(0.3);

    private record Registration(DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function, int maxTicks) {}

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        TESTS.add(new Registration(FUNCTIONS.register(name, () -> body), maxTicks));
    }

    static {
        test("cow_prefers_tall_grass", 400, HerbivoreGameTests::cowPrefersTallGrass);
        test("sheep_spare_the_last_flower", 400, HerbivoreGameTests::sheepSpareTheLastFlower);
        test("pig_eats_dropped_carrot", 400, HerbivoreGameTests::pigEatsDroppedCarrot);
        test("pig_eats_ripe_potatoes", 400, HerbivoreGameTests::pigEatsRipePotatoes);
        test("chicken_pecks_seeds", 400, HerbivoreGameTests::chickenPecksSeeds);
        test("rabbit_nibbles_flowers", 600, HerbivoreGameTests::rabbitNibblesFlowers);
        test("food_need_reads_fullness_and_plants", 40, HerbivoreGameTests::foodNeedReadsFullnessAndPlants);
        test("full_happy_cows_breed_on_their_own", 300, HerbivoreGameTests::fullHappyCowsBreed);
        test("hungry_cows_do_not_self_breed", 40, HerbivoreGameTests::hungryCowsDoNotSelfBreed);
        test("crowded_cows_do_not_self_breed", 40, HerbivoreGameTests::crowdedCowsDoNotSelfBreed);
        test("hungry_herd_moves_to_tall_grass", 400, HerbivoreGameTests::hungryHerdMovesToTallGrass);
        test("herd_leaves_where_it_was_attacked", 400, HerbivoreGameTests::herdLeavesWhereItWasAttacked);
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(HerbivoreGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("herbivores"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (Registration test : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.function().getId());
            event.registerTest(key.identifier(),
                    new FunctionGameTestInstance(key, new TestData<>(environment, id("herd_pen"), test.maxTicks(), 0, true)));
        }
    }

    /** A hungry cow with short grass 2 blocks one way and tall grass 4 blocks the other eats the tall grass first. */
    private static void cowPrefersTallGrass(GameTestHelper helper) {
        TestPens.build(helper);
        helper.setBlock(16, 1, 14, Blocks.SHORT_GRASS);
        tallGrass(helper, 10, 14);
        Cow cow = hungry(helper.spawn(EntityTypes.COW, 14, 1, 14), 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(10, 1, 14)).isAir(), "tall grass not eaten yet");
            helper.assertTrue(helper.getBlockState(new BlockPos(16, 1, 14)).is(Blocks.SHORT_GRASS), "ate the short grass first");
            helper.assertTrue(FullnessApi.fullness(cow).value() > HUNGRY.value(), "fullness did not go up");
        });
    }

    /** A hungry sheep next to two dandelions and one poppy eats one dandelion and leaves the rest. */
    private static void sheepSpareTheLastFlower(GameTestHelper helper) {
        TestPens.build(helper);
        helper.setBlock(12, 1, 14, Blocks.POPPY);
        helper.setBlock(16, 1, 14, Blocks.DANDELION);
        helper.setBlock(14, 1, 17, Blocks.DANDELION);
        hungry(helper.spawn(EntityTypes.SHEEP, 14, 1, 14), 1);
        helper.runAtTickTime(300, () -> {
            int dandelions = (helper.getBlockState(new BlockPos(16, 1, 14)).is(Blocks.DANDELION) ? 1 : 0)
                    + (helper.getBlockState(new BlockPos(14, 1, 17)).is(Blocks.DANDELION) ? 1 : 0);
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 1, 14)).is(Blocks.POPPY), "ate the last poppy");
            helper.assertTrue(dandelions == 1, "expected one dandelion eaten, " + dandelions + " left");
            helper.succeed();
        });
    }

    private static void pigEatsDroppedCarrot(GameTestHelper helper) {
        TestPens.build(helper);
        Pig pig = hungry(helper.spawn(EntityTypes.PIG, 14, 1, 14), 1);
        ItemEntity carrot = helper.spawnItem(Items.CARROT, 18.5F, 1.2F, 14.5F);
        carrot.setPickUpDelay(32767);
        helper.succeedWhen(() -> {
            helper.assertTrue(!carrot.isAlive(), "carrot still on the ground");
            helper.assertTrue(FullnessApi.fullness(pig).value() > HUNGRY.value(), "fullness did not go up");
        });
    }

    private static void pigEatsRipePotatoes(GameTestHelper helper) {
        TestPens.build(helper);
        helper.setBlock(17, 0, 14, Blocks.FARMLAND);
        helper.setBlock(17, 1, 14, ((CropBlock) Blocks.POTATOES).getStateForAge(7));
        helper.setBlock(11, 0, 14, Blocks.FARMLAND);
        helper.setBlock(11, 1, 14, ((CropBlock) Blocks.POTATOES).getStateForAge(2));
        Pig pig = hungry(helper.spawn(EntityTypes.PIG, 14, 1, 14), 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(!helper.getBlockState(new BlockPos(17, 1, 14)).is(Blocks.POTATOES), "ripe potatoes not eaten");
            helper.assertTrue(helper.getBlockState(new BlockPos(11, 1, 14)).is(Blocks.POTATOES), "ate unripe potatoes");
            helper.assertTrue(FullnessApi.fullness(pig).value() > HUNGRY.value(), "fullness did not go up");
        });
    }

    private static void chickenPecksSeeds(GameTestHelper helper) {
        TestPens.build(helper);
        Chicken chicken = hungry(helper.spawn(EntityTypes.CHICKEN, 14, 1, 14), 1);
        ItemEntity seeds = helper.spawnItem(Items.WHEAT_SEEDS, 17.5F, 1.2F, 16.5F);
        seeds.setPickUpDelay(32767);
        helper.succeedWhen(() -> {
            helper.assertTrue(!seeds.isAlive(), "seeds still on the ground");
            helper.assertTrue(FullnessApi.fullness(chicken).value() > HUNGRY.value(), "fullness did not go up");
        });
    }

    private static void rabbitNibblesFlowers(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 12; x <= 16; x += 2) {
            helper.setBlock(x, 1, 16, Blocks.CORNFLOWER);
        }
        Rabbit rabbit = hungry(helper.spawn(EntityTypes.RABBIT, 14, 1, 14), 1);
        helper.succeedWhen(() -> {
            int left = 0;
            for (int x = 12; x <= 16; x += 2) {
                left += helper.getBlockState(new BlockPos(x, 1, 16)).is(Blocks.CORNFLOWER) ? 1 : 0;
            }
            helper.assertTrue(left < 3, "no flower nibbled");
            helper.assertTrue(FullnessApi.fullness(rabbit).value() > HUNGRY.value(), "fullness did not go up");
        });
    }

    /** Food is half fullness, half plants nearby: a full cow on bare turf scores 0.5, on grass 1. */
    private static void foodNeedReadsFullnessAndPlants(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 1; x <= 26; x += 3) {
            for (int z = 15; z <= 21; z += 3) {
                helper.setBlock(x, 1, z, Blocks.SHORT_GRASS);
            }
        }
        Cow bare = helper.spawn(EntityTypes.COW, 14, 1, 4);
        Cow meadow = helper.spawn(EntityTypes.COW, 14, 1, 18);
        Cow starving = helper.spawn(EntityTypes.COW, 5, 1, 4);
        for (Cow cow : List.of(bare, meadow, starving)) {
            cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(1));
            FullnessApi.set(cow, cow.equals(starving) ? Fullness.EMPTY : Fullness.FULL);
        }
        helper.runAtTickTime(5, () -> {
            double bareFood = foodScore(helper, bare);
            double meadowFood = foodScore(helper, meadow);
            double starvingFood = foodScore(helper, starving);
            helper.assertTrue(Math.abs(bareFood - 0.5) < 1.0e-6, "full cow on bare turf: food " + bareFood);
            helper.assertTrue(Math.abs(meadowFood - 1.0) < 1.0e-6, "full cow among grass: food " + meadowFood);
            helper.assertTrue(starvingFood == 0.0, "starving cow on bare turf: food " + starvingFood);
            helper.succeed();
        });
    }

    /** Two full, thriving cows of one herd fall in love on their own and have a calf. */
    private static void fullHappyCowsBreed(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = breeders(helper, 2, Fullness.FULL);
        helper.runAtTickTime(2, () -> {
            SelfBreeding.Verdict verdict = SelfBreedingCheck.checkNow(cows.getFirst());
            helper.assertTrue(verdict == SelfBreeding.Verdict.READY, "verdict " + verdict);
            helper.assertTrue(cows.stream().allMatch(Animal::isInLove), "both cows should be in love");
            helper.assertTrue(cows.getFirst().getExistingData(HerbivoreAttachments.LAST_SELF_BRED)
                    .isPresent(), "cooldown not started");
        });
        helper.succeedWhen(() -> helper.assertTrue(
                helper.getEntities(EntityTypes.COW).stream().anyMatch(Cow::isBaby), "no calf yet"));
    }

    /** Merely fed (not full) cows don't breed on their own. */
    private static void hungryCowsDoNotSelfBreed(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = breeders(helper, 2, new Fullness(0.5));
        helper.runAtTickTime(2, () -> {
            SelfBreeding.Verdict verdict = SelfBreedingCheck.checkNow(cows.getFirst());
            helper.assertTrue(verdict == SelfBreeding.Verdict.NOT_WELL_ENOUGH, "verdict " + verdict);
            helper.assertTrue(cows.stream().noneMatch(Animal::isInLove), "fed cows fell in love");
            helper.succeed();
        });
    }

    /** Past the soft cap (12 cows within 16 blocks), nobody self-breeds. */
    private static void crowdedCowsDoNotSelfBreed(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = breeders(helper, 12, Fullness.FULL);
        helper.runAtTickTime(2, () -> {
            SelfBreeding.Verdict verdict = SelfBreedingCheck.checkNow(cows.getFirst());
            helper.assertTrue(verdict == SelfBreeding.Verdict.CROWDED, "verdict " + verdict);
            helper.assertTrue(cows.stream().noneMatch(Animal::isInLove), "crowded cows fell in love");
            helper.succeed();
        });
    }

    /** A hungry herd on bare turf moves its home to the patch of tall grass across the pen. */
    private static void hungryHerdMovesToTallGrass(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 18; x <= 22; x++) {
            for (int z = 18; z <= 22; z++) {
                tallGrass(helper, x, z);
            }
        }
        List<Cow> cows = List.of(hungry(helper.spawn(EntityTypes.COW, 5, 1, 5), 2), hungry(helper.spawn(EntityTypes.COW, 6, 1, 5), 2));
        Vec3 patch = helper.absoluteVec(new Vec3(20.5, 1, 20.5));
        helper.succeedWhen(() -> {
            Optional<HerdHome> home = homeOf(helper, cows.getFirst());
            double distance = home.map(h -> horizontal(h.anchor(), patch)).orElse(Double.POSITIVE_INFINITY);
            helper.assertTrue(distance < 6.0, "home is " + distance + " blocks from the tall grass");
        });
    }

    /** A cow is bitten where its herd lives: the herd's home moves out of the danger area. */
    private static void herdLeavesWhereItWasAttacked(GameTestHelper helper) {
        TestPens.build(helper);
        for (int x = 1; x < TestPens.SIZE - 1; x++) {
            for (int z = 1; z < TestPens.SIZE - 1; z += 2) {
                helper.setBlock(x, 1, z, Blocks.SHORT_GRASS);
            }
        }
        List<Cow> cows = List.of(fed(helper.spawn(EntityTypes.COW, 5, 1, 14), 2), fed(helper.spawn(EntityTypes.COW, 6, 1, 14), 2));
        List<Vec3> spot = new ArrayList<>();
        helper.runAtTickTime(45, () -> {
            Cow victim = cows.getFirst();
            helper.assertTrue(homeOf(helper, victim).isPresent(), "herd has no home yet");
            Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 1, 12);
            victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 1.0F);
            zombie.discard();
            helper.assertTrue(!DangerMemories.of(cows.getLast()).spots().isEmpty(), "herd mate did not remember the attack");
            spot.add(victim.position());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!spot.isEmpty(), "not attacked yet");
            double distance = homeOf(helper, cows.getFirst()).map(h -> horizontal(h.anchor(), spot.getFirst()))
                    .orElse(0.0);
            helper.assertTrue(distance >= 16.0, "home is only " + distance + " blocks from the attack");
        });
    }

    private static <A extends Animal> A hungry(A animal, int preference) {
        animal.setData(HerdAttachments.PREFERENCE, new HerdPreference(preference));
        FullnessApi.set(animal, HUNGRY);
        return animal;
    }

    private static <A extends Animal> A fed(A animal, int preference) {
        animal.setData(HerdAttachments.PREFERENCE, new HerdPreference(preference));
        FullnessApi.set(animal, Fullness.FULL);
        return animal;
    }

    /** {@code count} adult cows in one herd, thriving, at {@code fullness}. */
    private static List<Cow> breeders(GameTestHelper helper, int count, Fullness fullness) {
        HerdId herd = new HerdId(UUID.randomUUID());
        List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Cow cow = helper.spawn(EntityTypes.COW, 12 + i % 4 * 2, 1, 12 + i / 4 * 2);
            cow.setData(HerdAttachments.PREFERENCE, new HerdPreference(Math.max(2, count)));
            cow.setData(HerdAttachments.HERD, herd);
            cow.setData(WellbeingAttachments.STATE, thriving());
            FullnessApi.set(cow, fullness);
            cows.add(cow);
        }
        return cows;
    }

    private static WellbeingState thriving() {
        Map<Need, NeedScore> scores = new EnumMap<>(Need.class);
        for (Need need : Need.values()) {
            scores.put(need, NeedScore.FULL);
        }
        return new WellbeingState(new Wellbeing(90.0), new NeedScores(scores), RollingMemory.ALWAYS, RollingMemory.ALWAYS);
    }

    private static double foodScore(GameTestHelper helper, Cow cow) {
        WellbeingEvents.check(cow, helper.getLevel().getGameTime());
        return cow.getExistingData(WellbeingAttachments.STATE)
                .orElseThrow(() -> helper.assertionException("cow has no wellbeing after a check"))
                .needs().of(Need.FOOD).value();
    }

    private static Optional<HerdHome> homeOf(GameTestHelper helper, Animal animal) {
        return animal.getExistingData(HerdAttachments.HERD).flatMap(h -> HerdManager.homeOf(helper.getLevel(), h));
    }

    private static double horizontal(Position a, Vec3 b) {
        double dx = a.x() - b.x;
        double dz = a.z() - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void tallGrass(GameTestHelper helper, int x, int z) {
        Block tall = Blocks.TALL_GRASS;
        helper.setBlock(x, 2, z, tall.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(x, 1, z, tall.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
