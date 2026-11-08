package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionStatsStoreTest {
    @Test
    void keepsProfileAveragesSeparateAndResetsWithLobby() {
        SessionStatsStore store = new SessionStatsStore();
        store.record(finishedRun(1_000, 10_000), JadeProfile.PREPARED.key());
        store.record(finishedRun(100_000, 8_000), JadeProfile.PREPARED.key());
        store.record(finishedRun(200_000, 20_000), JadeProfile.IN_RUN.key());

        assertEquals(54_000, store.snapshot(JadeProfile.PREPARED.key()).averageRun());
        assertEquals(2, store.snapshot(JadeProfile.PREPARED.key()).runs());
        assertEquals(120_000, store.snapshot(JadeProfile.IN_RUN.key()).averageRun());

        store.reset();
        assertEquals(0, store.snapshot(JadeProfile.PREPARED.key()).runs());
    }

    private static NucleusRunTracker finishedRun(long start, long segment) {
        NucleusRunTracker tracker = new NucleusRunTracker();
        tracker.onNucleusExit(start);
        tracker.onCrystalFound(Crystal.JADE, start + segment);
        tracker.onCrystalFound(Crystal.AMBER, start + segment * 2);
        tracker.onCrystalFound(Crystal.TOPAZ, start + segment * 3);
        tracker.onCrystalFound(Crystal.SAPPHIRE, start + segment * 4);
        tracker.onCrystalFound(Crystal.AMETHYST, start + segment * 5);
        tracker.onNucleusCompleted(start + segment * 6);
        return tracker;
    }
}
