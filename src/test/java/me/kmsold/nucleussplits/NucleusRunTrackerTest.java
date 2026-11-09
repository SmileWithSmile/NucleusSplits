package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NucleusRunTrackerTest {
    @Test
    void firstCleanRunLearnsOrderAndDoesNotRestartOnAnotherExit() {
        NucleusRunTracker tracker = new NucleusRunTracker();
        tracker.onNucleusExit(1_000);
        tracker.onCrystalFound(Crystal.JADE, 11_000);
        tracker.onNucleusExit(12_000);
        tracker.onCrystalFound(Crystal.AMBER, 21_000);
        tracker.onCrystalFound(Crystal.TOPAZ, 31_000);
        tracker.onCrystalFound(Crystal.SAPPHIRE, 41_000);
        tracker.onCrystalFound(Crystal.AMETHYST, 51_000);
        tracker.onNucleusCompleted(61_000);

        assertEquals(List.of(Crystal.JADE, Crystal.AMBER, Crystal.TOPAZ, Crystal.SAPPHIRE, Crystal.AMETHYST),
                tracker.referenceOrder());
        assertEquals(60_000, tracker.elapsed(100_000));
        assertTrue(tracker.cleanFinish());
        assertFalse(tracker.routeMismatch());
        assertEquals(1, tracker.runNumber());
    }

    @Test
    void nextRunComparesAgainstLearnedSegmentsAndDetectsChangedOrder() {
        NucleusRunTracker tracker = learnedTracker();
        tracker.onNucleusExit(100_000);
        assertEquals(10_000, tracker.comparisonSegment(Crystal.JADE));
        tracker.onCrystalFound(Crystal.AMBER, 109_000);
        tracker.onCrystalFound(Crystal.JADE, 118_000);
        tracker.onCrystalFound(Crystal.TOPAZ, 127_000);
        tracker.onCrystalFound(Crystal.SAPPHIRE, 136_000);
        tracker.onCrystalFound(Crystal.AMETHYST, 145_000);
        tracker.onNucleusCompleted(154_000);

        assertTrue(tracker.routeMismatch());
        assertTrue(tracker.cleanFinish());
    }

    @Test
    void incompleteRunDoesNotBecomeReferenceAndDuplicateCrystalIsIgnored() {
        NucleusRunTracker tracker = new NucleusRunTracker();
        tracker.onNucleusExit(1_000);
        assertTrue(tracker.onCrystalFound(Crystal.JADE, 2_000));
        assertFalse(tracker.onCrystalFound(Crystal.JADE, 3_000));
        tracker.onNucleusCompleted(4_000);

        assertTrue(tracker.referenceOrder().isEmpty());
        assertFalse(tracker.cleanFinish());
    }

    @Test
    void resetForLobbyForgetsRouteAndBests() {
        NucleusRunTracker tracker = learnedTracker();
        tracker.resetLobby();
        assertTrue(tracker.referenceOrder().isEmpty());
        assertEquals(NucleusRunTracker.State.WAITING_FOR_EXIT, tracker.state());
        tracker.onNucleusExit(100_000);
        assertEquals(0, tracker.comparisonSegment(Crystal.JADE));
        assertEquals(1, tracker.runNumber());
    }

    @Test
    void cleanMatchingRunUpdatesSegmentBestsForFollowingRun() {
        NucleusRunTracker tracker = learnedTracker();
        tracker.onNucleusExit(100_000);
        tracker.onCrystalFound(Crystal.JADE, 109_000);
        tracker.onCrystalFound(Crystal.AMBER, 119_500);
        tracker.onCrystalFound(Crystal.TOPAZ, 128_500);
        tracker.onCrystalFound(Crystal.SAPPHIRE, 137_500);
        tracker.onCrystalFound(Crystal.AMETHYST, 146_500);
        tracker.onNucleusCompleted(155_500);
        tracker.onNucleusExit(200_000);

        assertEquals(9_000, tracker.comparisonSegment(Crystal.JADE));
        assertEquals(10_000, tracker.comparisonSegment(Crystal.AMBER));
        assertEquals(9_000, tracker.comparisonSegment(Crystal.TOPAZ));
        assertEquals(9_000, tracker.comparisonNucleusSegment());
    }

    @Test
    void abortedRunStopsElapsedTimeAndCannotComplete() {
        NucleusRunTracker tracker = new NucleusRunTracker();
        tracker.onNucleusExit(1_000);

        assertTrue(tracker.abort(31_000));
        assertEquals(NucleusRunTracker.State.ABORTED, tracker.state());
        assertEquals(30_000, tracker.elapsed(90_000));
        assertFalse(tracker.cleanFinish());
        assertFalse(tracker.onNucleusCompleted(100_000));
        assertEquals(0, tracker.totalTime());
    }

    @Test
    void nucleusBoundsIncludeDocumentedAreaEdges() {
        assertTrue(NucleusArea.contains(465, 64, 465));
        assertTrue(NucleusArea.contains(561, 188, 561));
        assertFalse(NucleusArea.contains(464.99, 100, 513));
        assertFalse(NucleusArea.contains(513, 63.99, 513));
    }

    private static NucleusRunTracker learnedTracker() {
        NucleusRunTracker tracker = new NucleusRunTracker();
        tracker.onNucleusExit(1_000);
        tracker.onCrystalFound(Crystal.JADE, 11_000);
        tracker.onCrystalFound(Crystal.AMBER, 21_000);
        tracker.onCrystalFound(Crystal.TOPAZ, 31_000);
        tracker.onCrystalFound(Crystal.SAPPHIRE, 41_000);
        tracker.onCrystalFound(Crystal.AMETHYST, 51_000);
        tracker.onNucleusCompleted(61_000);
        return tracker;
    }
}
