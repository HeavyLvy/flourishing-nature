package io.github.heavylvy.flourishingnature.neoforge.production;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.production.MilkSettings;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Tunables for production, in {@code flourishing_nature-production.toml}. */
public final class ProductionConfig {
    private ProductionConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue MILK = BUILDER
            .comment("Cows, mooshrooms and goats hold a limited number of milk charges that refill over time.")
            .define("milk.enabled", true);
    private static final ModConfigSpec.IntValue MAX_CHARGES = BUILDER
            .comment("Most milk charges an animal holds (a bucket of milk or a bowl of stew each).")
            .defineInRange("milk.maxCharges", MilkSettings.DEFAULT.maxCharges(), 1, 64);
    private static final ModConfigSpec.LongValue TICKS_PER_CHARGE = BUILDER
            .comment("Ticks to refill one charge at product rate 1 (content and fed). Thriving halves it,"
                    + " stressed triples it, hungry doubles it; miserable or starving never refills.")
            .defineInRange("milk.ticksPerCharge", MilkSettings.DEFAULT.ticksPerCharge(), 1L, 2_400_000L);
    private static final ModConfigSpec.BooleanValue DROPS = BUILDER
            .comment("Death drops scale with wellbeing and hunger (extra or fewer rolls, starving gives 1 meat).")
            .define("drops.enabled", true);
    private static final ModConfigSpec.BooleanValue WOOL = BUILDER
            .comment("Starving sheep give at most 1 wool, and hungry or stressed sheep regrow wool less often when they eat.")
            .define("wool.enabled", true);
    private static final ModConfigSpec.BooleanValue BREEDING = BUILDER
            .comment("Hungry or miserable animals refuse love mode; thriving parents rest less and their babies grow faster.")
            .define("breeding.enabled", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SYNCED, SPEC, ModIdentity.MOD_ID + "-production.toml");
    }

    /** Before the server config loads, defaults apply. */
    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    static boolean milk() {
        return !loaded() || MILK.getAsBoolean();
    }

    static boolean drops() {
        return !loaded() || DROPS.getAsBoolean();
    }

    static boolean wool() {
        return !loaded() || WOOL.getAsBoolean();
    }

    static boolean breeding() {
        return !loaded() || BREEDING.getAsBoolean();
    }

    static MilkSettings milkSettings() {
        return loaded() ? new MilkSettings(MAX_CHARGES.getAsInt(), TICKS_PER_CHARGE.getAsLong()) : MilkSettings.DEFAULT;
    }
}
