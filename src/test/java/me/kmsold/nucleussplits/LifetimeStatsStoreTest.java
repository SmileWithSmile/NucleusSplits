package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifetimeStatsStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesReloadsAndSeparatesFutureJadeProfiles() {
        Path file = temporaryDirectory.resolve("history.json");
        LifetimeStatsStore store = LifetimeStatsStore.load(file);
        NucleusRunTracker first = finishedRun(1_000, 10_000);
        NucleusRunTracker faster = finishedRun(100_000, 9_000);
        store.record(first, "unclassified");
        store.record(faster, "unclassified");
        store.record(first, "prepared_jade");

        LifetimeStatsStore.Snapshot unclassified = LifetimeStatsStore.load(file).snapshot("unclassified");
        LifetimeStatsStore.Snapshot prepared = LifetimeStatsStore.load(file).snapshot("prepared_jade");
        assertEquals(2, unclassified.runs());
        assertEquals(9_000, unclassified.segment(Crystal.JADE));
        assertEquals(54_000, unclassified.fullRun());
        assertEquals(1, prepared.runs());
        assertEquals(60_000, prepared.fullRun());
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
