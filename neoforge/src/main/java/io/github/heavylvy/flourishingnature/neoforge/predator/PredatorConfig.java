package io.github.heavylvy.flourishingnature.neoforge.predator;

import io.github.heavylvy.flourishingnature.core.predator.PackSettings;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server (synced) config {@code flourishing_nature-predators.toml}. Defaults are {@link PackSettings#DEFAULT}. */
final class PredatorConfig {
    private PredatorConfig() {}

    private static final double DAY = 24_000.0;
    private static final PackSettings D = PackSettings.DEFAULT;

    static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue HUNGER_GATES_HUNTING;
    private static final ModConfigSpec.BooleanValue KILLS_FEED_PACK;
    private static final ModConfigSpec.BooleanValue PACK_BREEDING;
    private static final ModConfigSpec.BooleanValue PACK_SPLITTING;
    private static final ModConfigSpec.DoubleValue SCENT_RADIUS;
    private static final ModConfigSpec.DoubleValue DRIFT_STEP;
    private static final ModConfigSpec.DoubleValue SHARE_RADIUS;
    private static final ModConfigSpec.DoubleValue BREED_COOLDOWN_DAYS;
    private static final ModConfigSpec.IntValue MAX_BREEDING_PACK_SIZE;
    private static final ModConfigSpec.DoubleValue SPLIT_AFTER_DAYS;
    private static final ModConfigSpec.DoubleValue DISPERSAL_MIN;
    private static final ModConfigSpec.DoubleValue DISPERSAL_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Wild (untamed) wolf packs and the food web.").push("wolves");
        HUNGER_GATES_HUNTING = b.comment("Wolves only hunt when hungry, preferring the weak and young. Off: vanilla hunting.")
                .define("hunger_gates_hunting", true);
        KILLS_FEED_PACK = b.comment("A kill fills the whole pack and its meat is eaten (no drops).")
                .define("kills_feed_pack", true);
        PACK_BREEDING = b.comment("A full pack has pups on its own.").define("pack_breeding", true);
        PACK_SPLITTING = b.comment("A starving pack without a kill for a while breaks up.").define("pack_splitting", true);
        SCENT_RADIUS = b.comment("A hungry pack drifts toward prey this many blocks from its home.")
                .defineInRange("scent_radius", D.scentRadius(), 8.0, 128.0);
        DRIFT_STEP = b.comment("Blocks the pack's home moves toward prey every 2 seconds.")
                .defineInRange("drift_step", D.driftStep(), 0.5, 16.0);
        SHARE_RADIUS = b.comment("Pack mates within this many blocks of a kill share the meal.")
                .defineInRange("share_radius", D.shareRadius(), 1.0, 64.0);
        BREED_COOLDOWN_DAYS = b.comment("In-game days a wolf waits between pups.")
                .defineInRange("breed_cooldown_days", D.breedCooldown() / DAY, 0.0, 30.0);
        MAX_BREEDING_PACK_SIZE = b.comment("Packs this big stop having pups.")
                .defineInRange("max_breeding_pack_size", D.maxBreedingPackSize(), 2, 32);
        SPLIT_AFTER_DAYS = b.comment("In-game days without a kill (and everyone starving) before a pack breaks up.")
                .defineInRange("split_after_days", D.splitAfter() / DAY, 0.1, 60.0);
        DISPERSAL_MIN = b.comment("A wolf leaving a broken pack heads at least this far away.")
                .defineInRange("dispersal_min", D.dispersalMin(), 8.0, 512.0);
        DISPERSAL_MAX = b.comment("... and at most this far.").defineInRange("dispersal_max", D.dispersalMax(), 8.0, 512.0);
        b.pop();
        SPEC = b.build();
    }

    static boolean hungerGatesHunting() {
        return !SPEC.isLoaded() || HUNGER_GATES_HUNTING.getAsBoolean();
    }

    static boolean killsFeedPack() {
        return !SPEC.isLoaded() || KILLS_FEED_PACK.getAsBoolean();
    }

    static boolean packBreeding() {
        return !SPEC.isLoaded() || PACK_BREEDING.getAsBoolean();
    }

    static boolean packSplitting() {
        return !SPEC.isLoaded() || PACK_SPLITTING.getAsBoolean();
    }

    static PackSettings settings() {
        if (!SPEC.isLoaded()) {
            return D;
        }
        double min = DISPERSAL_MIN.getAsDouble();
        return new PackSettings(
                SCENT_RADIUS.getAsDouble(),
                DRIFT_STEP.getAsDouble(),
                D.driftStopWithin(),
                SHARE_RADIUS.getAsDouble(),
                (long) (BREED_COOLDOWN_DAYS.getAsDouble() * DAY),
                MAX_BREEDING_PACK_SIZE.getAsInt(),
                (long) (SPLIT_AFTER_DAYS.getAsDouble() * DAY),
                min,
                Math.max(min, DISPERSAL_MAX.getAsDouble()),
                D.dispersalTime());
    }
}
