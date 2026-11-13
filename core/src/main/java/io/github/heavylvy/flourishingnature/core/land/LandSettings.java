package io.github.heavylvy.flourishingnature.core.land;

/**
 * Chances per sample of a spot. With the default sampler (one column per loaded chunk every 4
 * ticks) each column is sampled about every 1,024 ticks, roughly 23 times a game day.
 *
 * @param sproutShortGrass bare grass block to short grass (about once a day per block)
 * @param growTall short grass or fern to its tall form
 * @param spreadFlower a flower seeds a nearby free grass block
 * @param sapling tall grass to a sapling in a biome with tree growth 1.0; scaled by the biome's rate
 * @param loosenCoarseDirt coarse dirt next to tall grass back to dirt
 * @param creepMoss moss onto a neighbouring stone, cobblestone or gravel
 * @param gravelToDirt gravel under a moss carpet to dirt
 */
public record LandSettings(
        Chance sproutShortGrass,
        Chance growTall,
        Chance spreadFlower,
        Chance sapling,
        Chance loosenCoarseDirt,
        Chance creepMoss,
        Chance gravelToDirt) {

    /**
     * Starting guesses. The sapling chance is tuned so a forest clearing gets roughly one
     * sapling per 25 blocks in 20 days (25 x 20 x 23 samples x 0.0001 is about 1).
     */
    public static final LandSettings DEFAULT = new LandSettings(
            new Chance(0.04),
            new Chance(0.03),
            new Chance(0.02),
            new Chance(0.0001),
            new Chance(0.01),
            new Chance(0.01),
            new Chance(0.004));

    /** Every change certain: test hooks use this so a single forced step shows the effect. */
    public static final LandSettings ALWAYS = new LandSettings(
            Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS);
}
