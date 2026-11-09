package me.kmsold.nucleussplits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps every valid completed run. Lifetime PBs are derived from history, which lets a future
 * Prepared Jade / In-run Jade classifier split the same data into profiles without losing runs.
 */
public final class LifetimeStatsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Data data;
    private final Path file;

    private LifetimeStatsStore(Data data, Path file) {
        this.data = data;
        this.file = file;
    }

    public static LifetimeStatsStore load() {
        return load(FabricLoader.getInstance().getConfigDir().resolve("nucleus-splits-history.json"));
    }

    static LifetimeStatsStore load(Path file) {
        if (!Files.exists(file)) return new LifetimeStatsStore(new Data(), file);
        try (Reader reader = Files.newBufferedReader(file)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded == null) loaded = new Data();
            if (loaded.runs == null) loaded.runs = new ArrayList<>();
            loaded.schemaVersion = 2;
            for (StoredRun run : loaded.runs) {
                if (run.includedInAverage == null) run.includedInAverage = true;
            }
            return new LifetimeStatsStore(loaded, file);
        } catch (Exception exception) {
            NucleusSplits.LOGGER.warn("Could not read lifetime history {}, using an empty history", file, exception);
            return new LifetimeStatsStore(new Data(), file);
        }
    }

    public Snapshot snapshot(String profile) {
        String canonicalProfile = JadeProfile.fromKey(profile).key();
        EnumMap<Crystal, Long> segments = new EnumMap<>(Crystal.class);
        long nucleus = 0;
        long fullRun = 0;
        long totalTime = 0;
        int count = 0;
        int averageCount = 0;
        for (StoredRun run : data.runs) {
            if (!matchesProfile(canonicalProfile, run.profile)) continue;
            count++;
            if (run.segments != null) {
                for (Crystal crystal : Crystal.values()) {
                    Long value = run.segments.get(crystal.name());
                    if (value != null && value > 0) segments.merge(crystal, value, Math::min);
                }
            }
            nucleus = minPositive(nucleus, run.nucleusSegment);
            fullRun = minPositive(fullRun, run.totalTime);
            if (included(run) && run.totalTime > 0) {
                totalTime += run.totalTime;
                averageCount++;
            }
        }
        long averageRun = averageCount == 0 ? 0 : totalTime / averageCount;
        return new Snapshot(segments, nucleus, fullRun, averageRun, count, averageCount);
    }

    public long record(NucleusRunTracker tracker, String profile) {
        if (!tracker.cleanFinish() || tracker.state() != NucleusRunTracker.State.FINISHED) return -1;
        StoredRun run = new StoredRun();
        run.completedAt = Instant.now().toEpochMilli();
        run.profile = JadeProfile.fromKey(profile).key();
        run.order = tracker.observedOrder().stream().map(Crystal::name).toList();
        run.segments = new LinkedHashMap<>();
        for (Crystal crystal : tracker.observedOrder()) {
            run.segments.put(crystal.name(), tracker.segmentTime(crystal, run.completedAt));
        }
        run.nucleusSegment = tracker.nucleusSegment();
        run.totalTime = tracker.totalTime();
        run.includedInAverage = true;
        data.runs.add(run);
        save();
        return run.completedAt;
    }

    public List<RunEntry> runs(String profile) {
        String canonicalProfile = JadeProfile.fromKey(profile).key();
        List<RunEntry> result = new ArrayList<>();
        for (int index = 0; index < data.runs.size(); index++) {
            StoredRun run = data.runs.get(index);
            if (!matchesProfile(canonicalProfile, run.profile)) continue;
            result.add(new RunEntry(index, run.completedAt, run.totalTime, included(run)));
        }
        result.sort((left, right) -> Long.compare(right.completedAt, left.completedAt));
        return List.copyOf(result);
    }

    public void setIncludedInAverage(int historyIndex, boolean included) {
        if (historyIndex < 0 || historyIndex >= data.runs.size()) return;
        data.runs.get(historyIndex).includedInAverage = included;
        save();
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception exception) {
            NucleusSplits.LOGGER.error("Could not save lifetime history {}", file, exception);
        }
    }

    private static long minPositive(long oldValue, long newValue) {
        if (newValue <= 0) return oldValue;
        return oldValue <= 0 ? newValue : Math.min(oldValue, newValue);
    }

    private static boolean matchesProfile(String requested, String stored) {
        if (stored == null || stored.isBlank() || "unclassified".equals(stored)) {
            return JadeProfile.PREPARED.key().equals(requested);
        }
        return requested.equals(stored);
    }

    private static boolean included(StoredRun run) {
        return run.includedInAverage == null || run.includedInAverage;
    }

    public record Snapshot(Map<Crystal, Long> segments, long nucleusSegment, long fullRun,
                           long averageRun, int runs, int averageRuns) {
        public static Snapshot empty() {
            return new Snapshot(Map.of(), 0, 0, 0, 0, 0);
        }

        public long segment(Crystal crystal) {
            return segments.getOrDefault(crystal, 0L);
        }
    }

    public record RunEntry(int historyIndex, long completedAt, long totalTime, boolean includedInAverage) {}

    private static final class Data {
        int schemaVersion = 2;
        List<StoredRun> runs = new ArrayList<>();
    }

    private static final class StoredRun {
        long completedAt;
        String profile = "unclassified";
        List<String> order = List.of();
        Map<String, Long> segments = Map.of();
        long nucleusSegment;
        long totalTime;
        Boolean includedInAverage = true;
    }
}
