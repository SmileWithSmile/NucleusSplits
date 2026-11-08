package me.kmsold.nucleussplits;

public enum JadeProfile {
    PREPARED("prepared_jade", "nucleussplits.profile.prepared", "PREP"),
    IN_RUN("in_run_jade", "nucleussplits.profile.in_run", "IN-RUN");

    private final String key;
    private final String translationKey;
    private final String shortLabel;

    JadeProfile(String key, String translationKey, String shortLabel) {
        this.key = key;
        this.translationKey = translationKey;
        this.shortLabel = shortLabel;
    }

    public String key() {
        return key;
    }

    public String translationKey() {
        return translationKey;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public JadeProfile next() {
        return this == PREPARED ? IN_RUN : PREPARED;
    }

    public static JadeProfile fromKey(String key) {
        if (IN_RUN.key.equals(key)) return IN_RUN;
        // 0.3.0 stored every run as "unclassified". Keep those runs in the default
        // prepared-materials bucket instead of making existing history disappear.
        return PREPARED;
    }
}
