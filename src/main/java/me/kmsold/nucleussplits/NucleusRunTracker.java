package me.kmsold.nucleussplits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure timer state; Minecraft hooks only translate movement and chat into these events. */
public final class NucleusRunTracker {
    public enum State { WAITING_FOR_EXIT, RUNNING, FINISHED, ABORTED }

    private State state = State.WAITING_FOR_EXIT;
    private final List<Crystal> referenceOrder = new ArrayList<>();
    private final List<Crystal> observedOrder = new ArrayList<>();
    private final Set<Crystal> observed = new HashSet<>();
    private final EnumMap<Crystal, Long> cumulative = new EnumMap<>(Crystal.class);
    private final EnumMap<Crystal, Long> bestSegments = new EnumMap<>(Crystal.class);
    private final EnumMap<Crystal, Long> comparisonSegments = new EnumMap<>(Crystal.class);
    private long bestNucleusSegment;
    private long comparisonNucleusSegment;
    private long startMillis;
    private long finishMillis;
    private boolean routeMismatch;
    private boolean cleanFinish;
    private int runNumber;

    public void resetLobby() {
        state = State.WAITING_FOR_EXIT;
        referenceOrder.clear();
        bestSegments.clear();
        bestNucleusSegment = 0;
        runNumber = 0;
        clearRun();
    }

    public void onNucleusExit(long nowMillis) {
        if (state == State.RUNNING) return;
        clearRun();
        comparisonSegments.putAll(bestSegments);
        comparisonNucleusSegment = bestNucleusSegment;
        startMillis = nowMillis;
        state = State.RUNNING;
        runNumber++;
    }

    public boolean onCrystalFound(Crystal crystal, long nowMillis) {
        if (state != State.RUNNING || !observed.add(crystal)) return false;
        observedOrder.add(crystal);
        cumulative.put(crystal, elapsed(nowMillis));
        int index = observedOrder.size() - 1;
        if (!referenceOrder.isEmpty()
                && (index >= referenceOrder.size() || referenceOrder.get(index) != crystal)) {
            routeMismatch = true;
        }
        return true;
    }

    public boolean onNucleusCompleted(long nowMillis) {
        if (state != State.RUNNING) return false;
        finishMillis = elapsed(nowMillis);
        cleanFinish = observedOrder.size() == Crystal.values().length;
        if (cleanFinish && referenceOrder.isEmpty()) {
            referenceOrder.addAll(observedOrder);
            learnBests();
        } else if (cleanFinish && !routeMismatch && observedOrder.equals(referenceOrder)) {
            updateBests();
        }
        state = State.FINISHED;
        return true;
    }

    public boolean abort(long nowMillis) {
        if (state != State.RUNNING) return false;
        finishMillis = elapsed(nowMillis);
        cleanFinish = false;
        state = State.ABORTED;
        return true;
    }

    private void clearRun() {
        observedOrder.clear();
        observed.clear();
        cumulative.clear();
        comparisonSegments.clear();
        comparisonNucleusSegment = 0;
        startMillis = 0;
        finishMillis = 0;
        routeMismatch = false;
        cleanFinish = false;
    }

    private void learnBests() {
        long previous = 0;
        for (Crystal crystal : observedOrder) {
            long current = cumulative.get(crystal);
            bestSegments.put(crystal, current - previous);
            previous = current;
        }
        bestNucleusSegment = finishMillis - previous;
    }

    private void updateBests() {
        long previous = 0;
        for (Crystal crystal : observedOrder) {
            long current = cumulative.get(crystal);
            long segment = current - previous;
            bestSegments.merge(crystal, segment, Math::min);
            previous = current;
        }
        bestNucleusSegment = minPositive(bestNucleusSegment, finishMillis - previous);
    }

    private static long minPositive(long oldValue, long newValue) {
        return oldValue <= 0 ? newValue : Math.min(oldValue, newValue);
    }

    public long elapsed(long nowMillis) {
        if (startMillis == 0) return 0;
        return state == State.RUNNING ? Math.max(0, nowMillis - startMillis) : finishMillis;
    }

    public long segmentTime(Crystal crystal, long nowMillis) {
        int index = observedOrder.indexOf(crystal);
        if (index < 0) return 0;
        long previous = index == 0 ? 0 : cumulative.get(observedOrder.get(index - 1));
        return cumulative.get(crystal) - previous;
    }

    public long currentSegmentTime(long nowMillis) {
        long previous = observedOrder.isEmpty() ? 0 : cumulative.get(observedOrder.getLast());
        return Math.max(0, elapsed(nowMillis) - previous);
    }

    public long nucleusSegment() {
        if (state != State.FINISHED || observedOrder.isEmpty()) return 0;
        return finishMillis - cumulative.get(observedOrder.getLast());
    }

    public long totalTime() {
        return state == State.FINISHED ? finishMillis : 0;
    }

    public State state() { return state; }
    public List<Crystal> referenceOrder() { return Collections.unmodifiableList(referenceOrder); }
    public List<Crystal> observedOrder() { return Collections.unmodifiableList(observedOrder); }
    public Map<Crystal, Long> cumulativeTimes() { return Collections.unmodifiableMap(cumulative); }
    public long comparisonSegment(Crystal crystal) { return comparisonSegments.getOrDefault(crystal, 0L); }
    public long comparisonNucleusSegment() { return comparisonNucleusSegment; }
    public boolean routeMismatch() { return routeMismatch; }
    public boolean cleanFinish() { return cleanFinish; }
    public int runNumber() { return runNumber; }
}
