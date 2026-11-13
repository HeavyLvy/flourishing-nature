package io.github.heavylvy.flourishingnature.core.land;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LandRulesTest {
    private static Column column(Ground ground, Cover cover) {
        return new Column(ground, cover, true, false, false, false, TreeGrowth.FOREST, false, true);
    }

    private static LandChange always(Column column) {
        return LandRules.decide(column, LandSettings.ALWAYS, FixedRandomness.rolling(0.0));
    }

    @Test
    void grassGrowsUpThroughItsStages() {
        assertEquals(LandChange.SPROUT_SHORT_GRASS, always(column(Ground.GRASS_BLOCK, Cover.BARE)));
        assertEquals(LandChange.GROW_TALL, always(column(Ground.GRASS_BLOCK, Cover.SHORT_GRASS)));
        assertEquals(LandChange.GROW_TALL, always(column(Ground.GRASS_BLOCK, Cover.FERN)));
        assertEquals(LandChange.SPREAD_FLOWER, always(column(Ground.GRASS_BLOCK, Cover.FLOWER)));
        assertEquals(LandChange.SPROUT_SAPLING, always(column(Ground.GRASS_BLOCK, Cover.TALL_GRASS)));
    }

    @Test
    void bareDirtIsLeftToVanillaGrassSpread() {
        assertEquals(LandChange.NONE, always(column(Ground.DIRT, Cover.BARE)));
    }

    @Test
    void plantsNeedLightAndRoom() {
        Column dark = new Column(Ground.GRASS_BLOCK, Cover.BARE, false, false, false, false, TreeGrowth.FOREST, false, true);
        assertEquals(LandChange.NONE, always(dark));
        Column cramped = new Column(Ground.GRASS_BLOCK, Cover.SHORT_GRASS, true, false, false, false, TreeGrowth.FOREST, false, false);
        assertEquals(LandChange.NONE, always(cramped));
    }

    @Test
    void torchLitBasesNeverChange() {
        for (Ground ground : Ground.values()) {
            for (Cover cover : Cover.values()) {
                Column lit = new Column(ground, cover, true, true, true, true, TreeGrowth.FOREST, true, true);
                assertEquals(LandChange.NONE, always(lit), ground + " / " + cover);
            }
        }
    }

    @Test
    void saplingsFollowTheBiomeRate() {
        Column desert = new Column(Ground.GRASS_BLOCK, Cover.TALL_GRASS, true, false, false, false, TreeGrowth.NONE, false, true);
        assertEquals(LandChange.NONE, always(desert));
        LandSettings half = new LandSettings(Chance.NEVER, Chance.NEVER, Chance.NEVER, new Chance(0.5),
                Chance.NEVER, Chance.NEVER, Chance.NEVER);
        Column plains = new Column(Ground.GRASS_BLOCK, Cover.TALL_GRASS, true, false, false, false, new TreeGrowth(0.1), false, true);
        // At rate 0.1 the 0.5 chance becomes 0.05: a 0.06 roll misses, a 0.04 roll hits.
        assertEquals(LandChange.NONE, LandRules.decide(plains, half, FixedRandomness.rolling(0.06)));
        assertEquals(LandChange.SPROUT_SAPLING, LandRules.decide(plains, half, FixedRandomness.rolling(0.04)));
    }

    @Test
    void plainsGrowTreesTenTimesSlowerThanForests() {
        LandSettings settings = new LandSettings(Chance.NEVER, Chance.NEVER, Chance.NEVER, new Chance(0.01),
                Chance.NEVER, Chance.NEVER, Chance.NEVER);
        int forest = saplings(settings, TreeGrowth.FOREST);
        int plains = saplings(settings, new TreeGrowth(0.1));
        double ratio = (double) forest / plains;
        assertTrue(ratio > 7 && ratio < 14, "forest/plains sapling ratio " + ratio);
    }

    private static int saplings(LandSettings settings, TreeGrowth trees) {
        Column column = new Column(Ground.GRASS_BLOCK, Cover.TALL_GRASS, true, false, false, false, trees, false, true);
        FixedRandomness random = FixedRandomness.seeded(42);
        int count = 0;
        for (int i = 0; i < 200_000; i++) {
            if (LandRules.decide(column, settings, random) == LandChange.SPROUT_SAPLING) {
                count++;
            }
        }
        return count;
    }

    @Test
    void rainGivesGrowthASecondRoll() {
        LandSettings half = new LandSettings(new Chance(0.5), Chance.NEVER, Chance.NEVER, Chance.NEVER,
                Chance.NEVER, Chance.NEVER, Chance.NEVER);
        Column dry = column(Ground.GRASS_BLOCK, Cover.BARE);
        Column wet = new Column(Ground.GRASS_BLOCK, Cover.BARE, true, false, true, false, TreeGrowth.FOREST, false, true);
        // 0.5 dry; 0.75 in rain.
        assertEquals(LandChange.NONE, LandRules.decide(dry, half, FixedRandomness.rolling(0.6)));
        assertEquals(LandChange.SPROUT_SHORT_GRASS, LandRules.decide(wet, half, FixedRandomness.rolling(0.6)));
    }

    @Test
    void coarseDirtHealsOnlyFromTallGrassEdges() {
        assertEquals(LandChange.NONE, always(column(Ground.COARSE_DIRT, Cover.BARE)));
        Column edge = new Column(Ground.COARSE_DIRT, Cover.BARE, true, false, false, false, TreeGrowth.NONE, true, true);
        assertEquals(LandChange.LOOSEN_COARSE_DIRT, always(edge));
    }

    @Test
    void mossCreepsOnlyInHumidBiomes() {
        Column dry = column(Ground.MOSS_BLOCK, Cover.BARE);
        assertEquals(LandChange.NONE, always(dry));
        Column humid = new Column(Ground.MOSS_BLOCK, Cover.BARE, false, false, false, true, TreeGrowth.NONE, false, true);
        assertEquals(LandChange.CREEP_MOSS, always(humid));
        Column cobble = new Column(Ground.MOSSY_COBBLESTONE, Cover.BARE, false, false, false, true, TreeGrowth.NONE, false, true);
        assertEquals(LandChange.CREEP_MOSS, always(cobble));
        Column gravel = new Column(Ground.MOSSY_GRAVEL, Cover.OTHER, false, false, false, true, TreeGrowth.NONE, false, true);
        assertEquals(LandChange.GRAVEL_TO_DIRT, always(gravel));
    }

    @Test
    void chancesAndRatesAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new Chance(1.5));
        assertThrows(IllegalArgumentException.class, () -> new Chance(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TreeGrowth(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new Sampler(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TickSpan(5, 1));
        assertEquals(0.75, new Chance(0.5).twice().value(), 1e-9);
        assertEquals(1.0, new Chance(0.5).scaled(4).value(), 1e-9);
        assertFalse(Chance.NEVER.roll(FixedRandomness.rolling(0.0)));
    }

    @Test
    void defaultSamplerVisitsEachColumnAboutEveryThousandTicks() {
        assertEquals(1024.0, Sampler.DEFAULT.ticksPerColumnSample(), 1e-9);
        assertTrue(Sampler.DEFAULT.runsAt(8));
        assertFalse(Sampler.DEFAULT.runsAt(9));
        assertFalse(new Sampler(4, 0).runsAt(8));
    }
}
