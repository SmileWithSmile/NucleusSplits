package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfkRunPolicyTest {
    @Test
    void usesEightMinutesWhenThereIsNoAverage() {
        assertEquals(480_000, AfkRunPolicy.limit(0, 0));
    }

    @Test
    void usesOneAndAHalfTimesAllTimeAverage() {
        assertEquals(360_000, AfkRunPolicy.limit(240_000, 180_000));
    }

    @Test
    void fallsBackToSessionAverageAndNeverExceedsEightMinutes() {
        assertEquals(270_000, AfkRunPolicy.limit(0, 180_000));
        assertEquals(480_000, AfkRunPolicy.limit(400_000, 0));
    }
}
