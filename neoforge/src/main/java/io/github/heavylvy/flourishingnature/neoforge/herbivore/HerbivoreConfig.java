package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import com.mojang.logging.LogUtils;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerSettings;
import io.github.heavylvy.flourishingnature.core.herbivore.GrazingSettings;
import io.github.heavylvy.flourishingnature.core.herbivore.HomeSettings;
import io.github.heavylvy.flourishingnature.core.herbivore.PopulationCaps;
import io.github.heavylvy.flourishingnature.core.herbivore.SelfBreedingSettings;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

/** Server (synced) config {@code flourishing_nature-herbivores.toml}. Defaults match the core settings records. */
public final class HerbivoreConfig {
    private HerbivoreConfig() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue GRAZING = BUILDER
            .comment("Animals walk to real plants and eat them.")
            .define("grazing.enabled", true);
    private static final ModConfigSpec.DoubleValue GRAZE_BELOW = BUILDER
            .comment("Animals look for food while their fullness (0 to 1) is under this.")
            .defineInRange("grazing.grazeBelow", GrazingSettings.DEFAULT.grazeBelow(), 0.05, 1.0);
    private static final ModConfigSpec.DoubleValue VERY_HUNGRY_BELOW = BUILDER
            .comment("Cows crop grass blocks to dirt when their fullness is under this.")
            .defineInRange("grazing.veryHungryBelow", GrazingSettings.DEFAULT.veryHungryBelow(), 0.0, 1.0);
    private static final ModConfigSpec.IntValue SEARCH_RADIUS = BUILDER
            .comment("How far (blocks) an animal looks for food.")
            .defineInRange("grazing.searchRadius", GrazingSettings.DEFAULT.searchRadius(), 1, 16);
    private static final ModConfigSpec.IntValue SEARCH_INTERVAL = BUILDER
            .comment("Ticks between food searches per animal.")
            .defineInRange("grazing.searchInterval", GrazingSettings.DEFAULT.searchInterval(), 1, 2400);

    private static final ModConfigSpec.BooleanValue SELF_BREEDING = BUILDER
            .comment("Full, happy adults next to a herd mate in the same state breed on their own.")
            .define("selfBreeding.enabled", true);
    private static final ModConfigSpec.LongValue BREEDING_COOLDOWN = BUILDER
            .comment("Ticks between self-breedings for one animal (24000 = one day).")
            .defineInRange("selfBreeding.cooldownTicks", SelfBreedingSettings.DEFAULT.cooldownTicks(), 0L, 2_400_000L);
    private static final ModConfigSpec.DoubleValue CAP_RADIUS = BUILDER
            .comment("Soft population cap: animals of a species within this many blocks are counted.")
            .defineInRange("selfBreeding.capRadius", SelfBreedingSettings.DEFAULT.capRadius(), 2.0, 64.0);
    private static final ModConfigSpec.IntValue DEFAULT_CAP = BUILDER
            .comment("Soft population cap: no self-breeding where this many of the species are already within capRadius.")
            .defineInRange("selfBreeding.defaultCap", SelfBreedingSettings.DEFAULT.defaultCap(), 1, 256);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> CAPS = BUILDER
            .comment("Per-species caps, like \"minecraft:chicken=16\".")
            .defineList("selfBreeding.speciesCaps", List.of("minecraft:chicken=16", "minecraft:pig=8"), () -> "minecraft:cow=12",
                    o -> o instanceof String);

    private static final ModConfigSpec.BooleanValue HUNGRY_ROAM = BUILDER
            .comment("When the plants around a herd's home are gone, the herd moves its home toward food nearby.")
            .define("homes.followFood", true);
    private static final ModConfigSpec.IntValue SCARCE_AT = BUILDER
            .comment("A home with this many edible plants or fewer around it has run out of food.")
            .defineInRange("homes.scarceAt", HomeSettings.DEFAULT.scarceAt(), 0, 64);
    private static final ModConfigSpec.BooleanValue DANGER = BUILDER
            .comment("Herds remember where they were attacked and keep their homes away from it.")
            .define("danger.enabled", true);
    private static final ModConfigSpec.DoubleValue DANGER_RADIUS = BUILDER
            .comment("Herd homes stay this many blocks from a fresh danger spot.")
            .defineInRange("danger.radius", DangerSettings.DEFAULT.radius(), 1.0, 64.0);
    private static final ModConfigSpec.LongValue DANGER_HOLD = BUILDER
            .comment("Ticks a danger spot is avoided at full radius.")
            .defineInRange("danger.holdTicks", DangerSettings.DEFAULT.holdTicks(), 0L, 240_000L);
    private static final ModConfigSpec.LongValue DANGER_FADE = BUILDER
            .comment("Ticks over which the avoided area then shrinks to nothing.")
            .defineInRange("danger.fadeTicks", DangerSettings.DEFAULT.fadeTicks(), 1L, 240_000L);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SYNCED, SPEC, ModIdentity.MOD_ID + "-herbivores.toml");
    }

    /** Before the server config loads (it never is outside a running server), defaults apply. */
    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    static boolean grazing() {
        return !loaded() || GRAZING.getAsBoolean();
    }

    static boolean selfBreeding() {
        return !loaded() || SELF_BREEDING.getAsBoolean();
    }

    static boolean followFood() {
        return !loaded() || HUNGRY_ROAM.getAsBoolean();
    }

    static boolean dangerMemory() {
        return !loaded() || DANGER.getAsBoolean();
    }

    static GrazingSettings grazingSettings() {
        if (!loaded()) {
            return GrazingSettings.DEFAULT;
        }
        double below = GRAZE_BELOW.getAsDouble();
        return new GrazingSettings(below, Math.min(below, VERY_HUNGRY_BELOW.getAsDouble()), SEARCH_RADIUS.getAsInt(),
                SEARCH_INTERVAL.getAsInt(), GrazingSettings.DEFAULT.ediblePlantsReach());
    }

    static SelfBreedingSettings selfBreedingSettings() {
        SelfBreedingSettings d = SelfBreedingSettings.DEFAULT;
        return loaded()
                ? new SelfBreedingSettings(BREEDING_COOLDOWN.getAsLong(), d.checkInterval(), d.mateRadius(), CAP_RADIUS.getAsDouble(),
                        DEFAULT_CAP.getAsInt())
                : d;
    }

    static PopulationCaps populationCaps() {
        if (!loaded()) {
            return new PopulationCaps(SelfBreedingSettings.DEFAULT.defaultCap(), java.util.Map.of());
        }
        List<String> rejected = new ArrayList<>();
        PopulationCaps caps = PopulationCaps.parse(DEFAULT_CAP.getAsInt(), List.copyOf(CAPS.get()), rejected);
        rejected.forEach(line -> LOGGER.warn("Ignoring bad population cap '{}' (expected namespace:path=number)", line));
        return caps;
    }

    static HomeSettings homeSettings() {
        HomeSettings d = HomeSettings.DEFAULT;
        return loaded() ? new HomeSettings(SCARCE_AT.getAsInt(), d.foodCheckInterval(), d.patchRadii(), d.patchHalfWidth()) : d;
    }

    static DangerSettings dangerSettings() {
        DangerSettings d = DangerSettings.DEFAULT;
        return loaded()
                ? new DangerSettings(DANGER_RADIUS.getAsDouble(), DANGER_HOLD.getAsLong(), DANGER_FADE.getAsLong(), d.maxSpots(),
                        d.sameSpotWithin(), d.shareRadius())
                : d;
    }
}
