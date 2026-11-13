package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.core.nest.NestSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Server config for nests, saved as {@code flourishing_nature-nests.toml}. */
public final class NestConfig {
    public static final String FILE_NAME = "flourishing_nature-nests.toml";
    public static final ModConfigSpec SPEC;
    private static final NestConfig INSTANCE;

    static {
        Pair<NestConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(NestConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final ModConfigSpec.IntValue claimRadius;
    private final ModConfigSpec.IntValue gatherRadius;
    private final ModConfigSpec.IntValue buildPoints;
    private final ModConfigSpec.LongValue incubationTicks;
    private final ModConfigSpec.LongValue wildDecayTicks;
    private final ModConfigSpec.LongValue layIntervalTicks;
    private final ModConfigSpec.DoubleValue broodShare;
    private final ModConfigSpec.IntValue broodBoutTicks;

    private NestConfig(ModConfigSpec.Builder b) {
        NestSettings d = NestSettings.DEFAULT;
        claimRadius = b.comment("Blocks a hen without a nest looks for an empty one to claim")
                .defineInRange("claim_radius", d.claimRadius(), 1, 64);
        gatherRadius = b.comment("Blocks a hen looks for nest material (sticks, grass, leaf litter, wheat)")
                .defineInRange("gather_radius", d.gatherRadius(), 1, 32);
        buildPoints = b.comment("Material points a hen needs to build a nest (stick 1, short grass 1, tall grass 2, leaf litter 2, wheat 2)")
                .defineInRange("build_points", d.buildPoints(), 1, 64);
        incubationTicks = b.comment("Ticks a hen must sit on a fertile egg before it hatches")
                .defineInRange("incubation_ticks", d.incubationTicks(), 1L, 10_000_000L);
        wildDecayTicks = b.comment("Ticks a wild nest lasts with no hen around before it falls apart (crafted nests never do)")
                .defineInRange("wild_decay_ticks", d.wildDecayTicks(), 1L, 100_000_000L);
        layIntervalTicks = b.comment("Ticks between eggs for a content, fed hen; wellbeing and hunger scale this")
                .defineInRange("lay_interval_ticks", d.layIntervalTicks(), 1L, 10_000_000L);
        broodShare = b.comment("Chance a free hen with fertile eggs starts a sitting bout when she checks")
                .defineInRange("brood_share", d.broodShare(), 0.0, 1.0);
        broodBoutTicks = b.comment("How long one sitting bout lasts, in ticks")
                .defineInRange("brood_bout_ticks", d.broodBoutTicks(), 1, 100_000);
    }

    /** The current settings. Before the server config loads, the defaults. */
    public static NestSettings settings() {
        if (!SPEC.isLoaded()) {
            return NestSettings.DEFAULT;
        }
        NestConfig c = INSTANCE;
        return new NestSettings(c.claimRadius.get(), c.gatherRadius.get(), c.buildPoints.get(), c.incubationTicks.get(),
                c.wildDecayTicks.get(), c.layIntervalTicks.get(), c.broodShare.get(), c.broodBoutTicks.get());
    }
}
