package io.github.heavylvy.flourishingnature.neoforge.gametest;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.production.MilkCharges;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScore;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScores;
import io.github.heavylvy.flourishingnature.core.wellbeing.RollingMemory;
import io.github.heavylvy.flourishingnature.core.wellbeing.Wellbeing;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.production.Milking;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** In-game checks for milk charges, wellbeing drops, wool and breeding effects. */
public final class ProductionGameTests {
    private ProductionGameTests() {}

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, ModIdentity.MOD_ID);
    private static final List<Registration> TESTS = new ArrayList<>();
    private static final double CONTENT = 60.0;
    private static final double THRIVING = 95.0;

    private record Registration(DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function, int maxTicks) {}

    private static void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        TESTS.add(new Registration(FUNCTIONS.register(name, () -> body), maxTicks));
    }

    static {
        test("cow_gives_two_buckets_then_refuses", 40, ProductionGameTests::cowGivesTwoBucketsThenRefuses);
        test("milk_refills_by_mood_and_hunger", 40, ProductionGameTests::milkRefillsByMoodAndHunger);
        test("mooshroom_stew_uses_milk_charges", 40, ProductionGameTests::mooshroomStewUsesCharges);
        test("starving_cow_drops_one_meat", 40, ProductionGameTests::starvingCowDropsOneMeat);
        test("thriving_cow_drops_an_extra_roll", 40, ProductionGameTests::thrivingCowDropsExtraRoll);
        test("starving_sheep_give_one_wool", 40, ProductionGameTests::starvingSheepGiveOneWool);
        test("starving_sheep_do_not_regrow_wool", 40, ProductionGameTests::starvingSheepDoNotRegrowWool);
        test("hungry_cow_eats_but_refuses_love", 40, ProductionGameTests::hungryCowEatsButRefusesLove);
        test("thriving_parents_rest_less_and_babies_grow_faster", 600, ProductionGameTests::thrivingParentsBreedFaster);
        test("starving_cow_never_dies", 220, ProductionGameTests::starvingCowNeverDies);
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(ProductionGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(id("production"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (Registration test : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, test.function().getId());
            event.registerTest(key.identifier(),
                    new FunctionGameTestInstance(key, new TestData<>(environment, id("herd_pen"), test.maxTicks(), 0, true)));
        }
    }

    /** A content, full cow gives two buckets of milk; the third bucket stays empty. */
    private static void cowGivesTwoBucketsThenRefuses(GameTestHelper helper) {
        TestPens.build(helper);
        Cow cow = animal(helper, EntityTypes.COW, 14, 14, CONTENT, Fullness.FULL);
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.runAtTickTime(2, () -> {
            for (int i = 0; i < 2; i++) {
                ItemStack got = use(player, cow, Items.BUCKET);
                helper.assertTrue(got.is(Items.MILK_BUCKET), "bucket " + (i + 1) + " gave " + got);
            }
            ItemStack third = use(player, cow, Items.BUCKET);
            helper.assertTrue(third.is(Items.BUCKET), "third bucket gave " + third);
            helper.succeed();
        });
    }

    /** One day after an empty udder: a content fed cow has 1 charge, a starving one none, a thriving one 2. */
    private static void milkRefillsByMoodAndHunger(GameTestHelper helper) {
        TestPens.build(helper);
        Cow fed = animal(helper, EntityTypes.COW, 8, 8, CONTENT, new Fullness(0.6));
        Cow starving = animal(helper, EntityTypes.COW, 14, 14, CONTENT, Fullness.EMPTY);
        Cow thriving = animal(helper, EntityTypes.COW, 20, 20, THRIVING, Fullness.FULL);
        helper.runAtTickTime(2, () -> {
            long dayAgo = helper.getLevel().getGameTime() - 24_000L;
            for (Cow cow : List.of(fed, starving, thriving)) {
                Milking.setCharges(cow, new MilkCharges(0, 0.0, dayAgo));
            }
            helper.assertTrue(Milking.charges(fed).charges() == 1, "fed " + Milking.charges(fed));
            helper.assertTrue(Milking.charges(starving).charges() == 0, "starving " + Milking.charges(starving));
            helper.assertTrue(Milking.charges(thriving).charges() == 2, "thriving " + Milking.charges(thriving));
            helper.succeed();
        });
    }

    /** A mooshroom with an empty udder keeps the bowl empty; with a charge it fills it with stew. */
    private static void mooshroomStewUsesCharges(GameTestHelper helper) {
        TestPens.build(helper);
        MushroomCow mooshroom = animal(helper, EntityTypes.MOOSHROOM, 14, 14, CONTENT, Fullness.FULL);
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.runAtTickTime(2, () -> {
            long now = helper.getLevel().getGameTime();
            Milking.setCharges(mooshroom, new MilkCharges(0, 0.0, now));
            ItemStack refused = use(player, mooshroom, Items.BOWL);
            helper.assertTrue(refused.is(Items.BOWL), "empty mooshroom gave " + refused);
            Milking.setCharges(mooshroom, new MilkCharges(1, 0.0, now));
            ItemStack stew = use(player, mooshroom, Items.BOWL);
            helper.assertTrue(stew.is(Items.MUSHROOM_STEW), "mooshroom with a charge gave " + stew);
            helper.succeed();
        });
    }

    /** Starving cows drop exactly one beef and no leather. */
    private static void starvingCowDropsOneMeat(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cows.add(animal(helper, EntityTypes.COW, 6 + 5 * i, 14, CONTENT, Fullness.EMPTY));
        }
        helper.runAtTickTime(2, () -> cows.forEach(cow -> cow.kill(helper.getLevel())));
        helper.runAtTickTime(4, () -> {
            int beef = dropped(helper, Items.BEEF);
            int leather = dropped(helper, Items.LEATHER);
            helper.assertTrue(beef == 4 && leather == 0, "4 starving cows dropped " + beef + " beef and " + leather + " leather");
            helper.succeed();
        });
    }

    /** A thriving, full cow rolls its loot twice: at least 2 beef (vanilla can drop 1). */
    private static void thrivingCowDropsExtraRoll(GameTestHelper helper) {
        TestPens.build(helper);
        List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cows.add(animal(helper, EntityTypes.COW, 6 + 5 * i, 14, THRIVING, Fullness.FULL));
        }
        helper.runAtTickTime(2, () -> cows.forEach(cow -> cow.kill(helper.getLevel())));
        helper.runAtTickTime(4, () -> {
            int beef = dropped(helper, Items.BEEF);
            helper.assertTrue(beef >= 8, "4 thriving cows dropped only " + beef + " beef");
            helper.succeed();
        });
    }

    /** Four starving sheep sheared give one wool each (vanilla gives 1 to 3). */
    private static void starvingSheepGiveOneWool(GameTestHelper helper) {
        TestPens.build(helper);
        List<Sheep> flock = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            flock.add(animal(helper, EntityTypes.SHEEP, 6 + 5 * i, 14, CONTENT, Fullness.EMPTY));
        }
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.runAtTickTime(2, () -> flock.forEach(sheep -> use(player, sheep, Items.SHEARS)));
        helper.runAtTickTime(4, () -> {
            int wool = droppedWool(helper);
            helper.assertTrue(flock.stream().allMatch(Sheep::isSheared), "not every sheep was sheared");
            helper.assertTrue(wool == 4, "4 starving sheep gave " + wool + " wool");
            helper.succeed();
        });
    }

    /** A sheared starving sheep that eats stays bare; a sheared content, fed one regrows its wool. */
    private static void starvingSheepDoNotRegrowWool(GameTestHelper helper) {
        TestPens.build(helper);
        Sheep starving = animal(helper, EntityTypes.SHEEP, 8, 14, CONTENT, Fullness.EMPTY);
        Sheep fed = animal(helper, EntityTypes.SHEEP, 20, 14, CONTENT, Fullness.FULL);
        starving.setSheared(true);
        fed.setSheared(true);
        helper.runAtTickTime(3, () -> {
            starving.ate();
            fed.ate();
        });
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(starving.isSheared(), "starving sheep regrew its wool");
            helper.assertTrue(!fed.isSheared(), "fed sheep did not regrow its wool");
            helper.succeed();
        });
    }

    /** Wheat fed to a hungry cow fills it but starts no love mode; a fed cow falls in love. */
    private static void hungryCowEatsButRefusesLove(GameTestHelper helper) {
        TestPens.build(helper);
        Cow hungry = animal(helper, EntityTypes.COW, 8, 14, CONTENT, new Fullness(0.3));
        Cow fed = animal(helper, EntityTypes.COW, 20, 14, CONTENT, new Fullness(0.6));
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.runAtTickTime(2, () -> {
            ItemStack left = use(player, hungry, Items.WHEAT);
            helper.assertTrue(!left.is(Items.WHEAT), "the hungry cow did not eat the wheat");
            helper.assertTrue(!hungry.isInLove(), "a hungry cow fell in love");
            helper.assertTrue(FullnessApi.fullness(hungry).value() > 0.5, "feeding did not fill it: " + FullnessApi.fullness(hungry));
            use(player, fed, Items.WHEAT);
            helper.assertTrue(fed.isInLove(), "a fed cow did not fall in love");
            helper.succeed();
        });
    }

    /** Thriving parents: cooldown 4500 instead of 6000, and the calf needs 19200 ticks instead of 24000. */
    private static void thrivingParentsBreedFaster(GameTestHelper helper) {
        TestPens.build(helper);
        Cow a = animal(helper, EntityTypes.COW, 13, 14, THRIVING, Fullness.FULL);
        Cow b = animal(helper, EntityTypes.COW, 15, 14, THRIVING, Fullness.FULL);
        helper.runAtTickTime(2, () -> {
            a.setInLove(helper.makeMockServerPlayer(GameType.SURVIVAL));
            b.setInLove(helper.makeMockServerPlayer(GameType.SURVIVAL));
        });
        helper.succeedWhen(() -> {
            List<Cow> calves = helper.getLevel().getEntitiesOfClass(Cow.class, bounds(helper), Cow::isBaby);
            helper.assertTrue(calves.size() == 1, "calves: " + calves.size());
            int calfAge = calves.getFirst().getAge();
            helper.assertTrue(calfAge > -20_000, "calf age " + calfAge + " (vanilla starts at -24000)");
            helper.assertTrue(a.getAge() > 0 && a.getAge() <= 4_600, "parent a cooldown " + a.getAge());
            helper.assertTrue(b.getAge() > 0 && b.getAge() <= 4_600, "parent b cooldown " + b.getAge());
        });
    }

    /** Ten seconds empty: no damage, still alive. */
    private static void starvingCowNeverDies(GameTestHelper helper) {
        TestPens.build(helper);
        Cow cow = animal(helper, EntityTypes.COW, 14, 14, CONTENT, Fullness.EMPTY);
        helper.runAtTickTime(200, () -> {
            helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth(), "starving cow lost health: " + cow.getHealth());
            helper.succeed();
        });
    }

    private static <T extends Animal> T animal(
            GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, int x, int z, double wellbeing, Fullness fullness) {
        T animal = helper.spawn(type, x, 1, z);
        animal.setData(WellbeingAttachments.STATE, state(wellbeing));
        FullnessApi.set(animal, fullness);
        return animal;
    }

    /** Uses one {@code item} on the animal and returns what the hand holds afterwards. */
    private static ItemStack use(Player player, Animal animal, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        player.interactOn(animal, InteractionHand.MAIN_HAND, Vec3.ZERO);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds(helper), e -> e.getItem().is(item))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static int droppedWool(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds(helper), e -> e.getItem().is(ItemTags.WOOL))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static AABB bounds(GameTestHelper helper) {
        return new AABB(helper.absoluteVec(new Vec3(0, -1, 0)), helper.absoluteVec(new Vec3(TestPens.SIZE, 6, TestPens.SIZE)));
    }

    private static WellbeingState state(double wellbeing) {
        Map<Need, NeedScore> scores = new EnumMap<>(Need.class);
        for (Need need : Need.values()) {
            scores.put(need, NeedScore.FULL);
        }
        return new WellbeingState(new Wellbeing(wellbeing), new NeedScores(scores), RollingMemory.ALWAYS, RollingMemory.ALWAYS);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
