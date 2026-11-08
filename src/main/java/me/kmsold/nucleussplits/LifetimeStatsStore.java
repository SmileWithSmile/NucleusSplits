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
            return new LifetimeStatsStore(loaded, file);
        } catch (Exception exception) {
            NucleusSplits.LOGGER.warn("Could not read lifetime history {}, using an empty history", file, exception);
            return new LifetimeStatsStore(new Data(), file);
        }
    }

    public Snapshot snapshot(String profile) {
        EnumMap<Crystal, Long> segments = new EnumMap<>(Crystal.class);
        long nucleus = 0;
        long fullRun = 0;
        int count = 0;
        for (StoredRun run : data.runs) {
            if (!profile.equals(run.profile)) continue;
            count++;
            if (run.segments != null) {
                for (Crystal crystal : Crystal.values()) {
                    Long value = run.segments.get(crystal.name());
                    if (value != null && value > 0) segments.merge(crystal, value, Math::min);
                }
            }
            nucleus = minPositive(nucleus, run.nucleusSegment);
            fullRun = minPositive(fullRun, run.totalTime);
        }
        return new Snapshot(segments, nucleus, fullRun, count);
    }

    public void record(NucleusRunTracker tracker, String profile) {
        if (!tracker.cleanFinish() || tracker.state() != NucleusRunTracker.State.FINISHED) return;
        StoredRun run = new StoredRun();
        run.completedAt = Instant.now().toEpochMilli();
        run.profile = profile;
        run.order = tracker.observedOrder().stream().map(Crystal::name).toList();
        run.segments = new LinkedHashMap<>();
        for (Crystal crystal : tracker.observedOrder()) {
            run.segments.put(crystal.name(), tracker.segmentTime(crystal, run.completedAt));
        }
        run.nucleusSegment = tracker.nucleusSegment();
        run.totalTime = tracker.totalTime();
        data.runs.add(run);
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

    public record Snapshot(Map<Crystal, Long> segments, long nucleusSegment, long fullRun, int runs) {
        public static Snapshot empty() {
            return new Snapshot(Map.of(), 0, 0, 0);
        }

        public long segment(Crystal crystal) {
            return segments.getOrDefault(crystal, 0L);
        }
    }

    private static final class Data {
        int schemaVersion = 1;
        List<StoredRun> runs = new ArrayList<>();
    }

    private static final class StoredRun {
        long completedAt;
        String profile = "unclassified";
        List<String> order = List.of();
        Map<String, Long> segments = Map.of();
        long nucleusSegment;
        long totalTime;
    }
}
