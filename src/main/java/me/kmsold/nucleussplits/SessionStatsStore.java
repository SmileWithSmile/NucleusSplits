package me.kmsold.nucleussplits;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** In-memory run history for the current Crystal Hollows lobby, separated by Jade profile. */
final class SessionStatsStore {
    private final List<SessionRun> runs = new ArrayList<>();

    LifetimeStatsStore.Snapshot snapshot(String profile) {
        String canonicalProfile = JadeProfile.fromKey(profile).key();
        EnumMap<Crystal, Long> segments = new EnumMap<>(Crystal.class);
        long nucleus = 0;
        long fullRun = 0;
        long totalTime = 0;
        int count = 0;
        int averageCount = 0;
        for (SessionRun run : runs) {
            if (!canonicalProfile.equals(run.profile)) continue;
            count++;
            for (Map.Entry<Crystal, Long> segment : run.segments.entrySet()) {
                segments.merge(segment.getKey(), segment.getValue(), Math::min);
            }
            nucleus = minPositive(nucleus, run.nucleusSegment);
            fullRun = minPositive(fullRun, run.totalTime);
            if (run.includedInAverage) {
                totalTime += run.totalTime;
                averageCount++;
            }
        }
        long average = averageCount == 0 ? 0 : totalTime / averageCount;
        return new LifetimeStatsStore.Snapshot(
                Map.copyOf(segments), nucleus, fullRun, average, count, averageCount);
    }

    void record(NucleusRunTracker tracker, String profile, long completedAt) {
        if (!tracker.cleanFinish() || tracker.state() != NucleusRunTracker.State.FINISHED) return;
        EnumMap<Crystal, Long> segments = new EnumMap<>(Crystal.class);
        for (Crystal crystal : tracker.observedOrder()) {
            segments.put(crystal, tracker.segmentTime(crystal, 0));
        }
        runs.add(new SessionRun(completedAt, JadeProfile.fromKey(profile).key(),
                Map.copyOf(segments), tracker.nucleusSegment(), tracker.totalTime(), true));
    }

    void setIncludedInAverage(long completedAt, boolean included) {
        for (int index = 0; index < runs.size(); index++) {
            SessionRun run = runs.get(index);
            if (run.completedAt == completedAt) {
                runs.set(index, new SessionRun(run.completedAt, run.profile, run.segments,
                        run.nucleusSegment, run.totalTime, included));
                return;
            }
        }
    }

    void reset() {
        runs.clear();
    }

    private static long minPositive(long oldValue, long newValue) {
        if (newValue <= 0) return oldValue;
        return oldValue <= 0 ? newValue : Math.min(oldValue, newValue);
    }

    private record SessionRun(long completedAt, String profile, Map<Crystal, Long> segments,
                              long nucleusSegment, long totalTime, boolean includedInAverage) {}
}
