package me.kmsold.nucleussplits;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** In-memory aggregates for the current Crystal Hollows lobby, separated by Jade profile. */
final class SessionStatsStore {
    private final Map<String, Aggregate> profiles = new HashMap<>();

    LifetimeStatsStore.Snapshot snapshot(String profile) {
        Aggregate aggregate = profiles.get(JadeProfile.fromKey(profile).key());
        if (aggregate == null) return LifetimeStatsStore.Snapshot.empty();
        long average = aggregate.runs == 0 ? 0 : aggregate.totalTime / aggregate.runs;
        return new LifetimeStatsStore.Snapshot(
                Map.copyOf(aggregate.segments), aggregate.nucleusSegment,
                aggregate.fullRun, average, aggregate.runs);
    }

    void record(NucleusRunTracker tracker, String profile) {
        if (!tracker.cleanFinish() || tracker.state() != NucleusRunTracker.State.FINISHED) return;
        Aggregate aggregate = profiles.computeIfAbsent(
                JadeProfile.fromKey(profile).key(), ignored -> new Aggregate());
        for (Crystal crystal : tracker.observedOrder()) {
            aggregate.segments.merge(crystal, tracker.segmentTime(crystal, 0), Math::min);
        }
        aggregate.nucleusSegment = minPositive(aggregate.nucleusSegment, tracker.nucleusSegment());
        aggregate.fullRun = minPositive(aggregate.fullRun, tracker.totalTime());
        aggregate.totalTime += tracker.totalTime();
        aggregate.runs++;
    }

    void reset() {
        profiles.clear();
    }

    private static long minPositive(long oldValue, long newValue) {
        if (newValue <= 0) return oldValue;
        return oldValue <= 0 ? newValue : Math.min(oldValue, newValue);
    }

    private static final class Aggregate {
        final EnumMap<Crystal, Long> segments = new EnumMap<>(Crystal.class);
        long nucleusSegment;
        long fullRun;
        long totalTime;
        int runs;
    }
}
