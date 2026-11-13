package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HerdAlarmTest {
    private static final SpeciesProfile COW = BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow();
    private static final HerdMember SCARED_COW = new HerdMember(COW, new HerdPreference(5), new Position(0, 64, 0));

    private static Neighbor at(SpeciesId species, double x) {
        return new Neighbor(species, new HerdPreference(5), new Position(x, 64, 0));
    }

    @Test
    void nearbyHerdMatesPanicToo() {
        assertTrue(HerdAlarm.spreadsTo(SCARED_COW, at(BuiltInProfiles.COW, 6), HerdingSettings.DEFAULT));
        assertTrue(HerdAlarm.spreadsTo(SCARED_COW, at(BuiltInProfiles.SHEEP, 6), HerdingSettings.DEFAULT));
    }

    @Test
    void farAwayMatesDoNotNotice() {
        assertFalse(HerdAlarm.spreadsTo(SCARED_COW, at(BuiltInProfiles.COW, 11), HerdingSettings.DEFAULT));
    }

    @Test
    void unrelatedSpeciesDoNotCare() {
        assertFalse(HerdAlarm.spreadsTo(SCARED_COW, at(BuiltInProfiles.PIG, 3), HerdingSettings.DEFAULT));
    }

    @Test
    void alarmExpires() {
        PanicAlarm alarm = PanicAlarm.unknownChaser(new Position(0, 64, 0), 100);
        assertTrue(alarm.isActive(99));
        assertFalse(alarm.isActive(100));
    }
}
