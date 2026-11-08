package me.kmsold.nucleussplits;

/** Approximate server area bounds documented for the fixed Crystal Nucleus at 513, 513. */
public final class NucleusArea {
    private NucleusArea() {}

    public static boolean contains(double x, double y, double z) {
        return x >= 465 && x <= 561 && y >= 64 && y <= 188 && z >= 465 && z <= 561;
    }
}
