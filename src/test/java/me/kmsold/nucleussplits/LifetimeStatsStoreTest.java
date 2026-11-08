package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifetimeStatsStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesReloadsSeparatesJadeProfilesAndCalculatesAverage() {
        Path file = temporaryDirectory.resolve("history.json");
        LifetimeStatsStore store = LifetimeStatsStore.load(file);
        NucleusRunTracker first = finishedRun(1_000, 10_000);
        NucleusRunTracker faster = finishedRun(100_000, 9_000);
        store.record(first, JadeProfile.PREPARED.key());
        store.record(faster, JadeProfile.PREPARED.key());
        store.record(first, JadeProfile.IN_RUN.key());

        LifetimeStatsStore.Snapshot prepared = LifetimeStatsStore.load(file).snapshot(JadeProfile.PREPARED.key());
        LifetimeStatsStore.Snapshot inRun = LifetimeStatsStore.load(file).snapshot(JadeProfile.IN_RUN.key());
        assertEquals(2, prepared.runs());
        assertEquals(9_000, prepared.segment(Crystal.JADE));
        assertEquals(54_000, prepared.fullRun());
        assertEquals(57_000, prepared.averageRun());
        assertEquals(1, inRun.runs());
        assertEquals(60_000, inRun.fullRun());
        assertEquals(60_000, inRun.averageRun());
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
