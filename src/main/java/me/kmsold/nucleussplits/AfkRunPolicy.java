package me.kmsold.nucleussplits;

final class AfkRunPolicy {
    static final long HARD_LIMIT_MILLIS = 8 * 60_000L;

    private AfkRunPolicy() {}

    static long limit(long allTimeAverage, long sessionAverage) {
        long average = allTimeAverage > 0 ? allTimeAverage : sessionAverage;
        if (average <= 0) return HARD_LIMIT_MILLIS;
        return Math.min(average + average / 2, HARD_LIMIT_MILLIS);
    }
}
