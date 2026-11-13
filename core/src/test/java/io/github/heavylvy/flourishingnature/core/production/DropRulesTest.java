package io.github.heavylvy.flourishingnature.core.production;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DropRulesTest {
    private static final DropStack BEEF_3 = new DropStack(true, 3);
    private static final DropStack LEATHER_2 = new DropStack(false, 2);

    @Test
    void minimumIsOneMeatAndNothingElse() {
        assertEquals(List.of(new DropStack(false, 0), new DropStack(true, 1), new DropStack(true, 0)),
                DropRules.minimum(List.of(LEATHER_2, BEEF_3, BEEF_3)));
        assertEquals(List.of(new DropStack(false, 0)), DropRules.minimum(List.of(LEATHER_2)), "no meat, nothing");
    }

    @Test
    void fewerRollsTakeOneFromEachStack() {
        assertEquals(List.of(new DropStack(true, 2), new DropStack(false, 1)),
                DropRules.fewerRolls(List.of(BEEF_3, LEATHER_2), 1));
        assertEquals(List.of(BEEF_3, LEATHER_2), DropRules.fewerRolls(List.of(BEEF_3, LEATHER_2), 0));
    }

    @Test
    void fewerRollsNeverGoBelowTheMinimum() {
        assertEquals(List.of(new DropStack(true, 1), new DropStack(false, 0)),
                DropRules.fewerRolls(List.of(new DropStack(true, 1), LEATHER_2), 3));
    }

    @Test
    void thrivingShortensCooldownAndSpeedsGrowth() {
        assertEquals(0.75, ProductionRules.breedingCooldownFactor(Mood.THRIVING));
        assertEquals(1.0, ProductionRules.breedingCooldownFactor(Mood.CONTENT));
        assertEquals(1.25, ProductionRules.babyGrowthSpeed(Mood.THRIVING));
        assertEquals(1.0, ProductionRules.babyGrowthSpeed(Mood.STRESSED));
    }

    @Test
    void woolRegrowthFollowsProductRateCappedAtVanilla() {
        assertEquals(1.0, ProductionRules.woolRegrowChance(Mood.THRIVING, HungerBand.FULL));
        assertEquals(0.5, ProductionRules.woolRegrowChance(Mood.CONTENT, HungerBand.HUNGRY));
        assertEquals(0.0, ProductionRules.woolRegrowChance(Mood.CONTENT, HungerBand.STARVING));
    }
}
