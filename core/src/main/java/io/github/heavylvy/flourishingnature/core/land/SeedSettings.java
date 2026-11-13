package io.github.heavylvy.flourishingnature.core.land;

/**
 * Sweet berries carried by animals and planted where they fall.
 *
 * @param foxDrop per check (every 40 ticks), a fox holding sweet berries drops them
 * @param chickenPeck per check, a chicken beside a ripe bush pecks a berry off to carry
 * @param chickenCarry how long a chicken carries a berry before dropping it
 * @param plantBerry per check (every 100 ticks), a sweet berry item lying on grass plants a bush
 */
public record SeedSettings(Chance foxDrop, Chance chickenPeck, TickSpan chickenCarry, Chance plantBerry) {
    public static final SeedSettings DEFAULT =
            new SeedSettings(new Chance(0.02), new Chance(0.02), new TickSpan(600, 2400), new Chance(0.05));

    public static final SeedSettings ALWAYS =
            new SeedSettings(Chance.ALWAYS, Chance.ALWAYS, new TickSpan(0, 0), Chance.ALWAYS);
}
