package me.kmsold.nucleussplits;

public enum StatsView {
    SESSION,
    LIFETIME;

    public StatsView next() {
        return this == SESSION ? LIFETIME : SESSION;
    }
}
