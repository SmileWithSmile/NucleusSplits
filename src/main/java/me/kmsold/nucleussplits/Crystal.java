package me.kmsold.nucleussplits;

import java.util.Locale;
import java.util.Optional;

public enum Crystal {
    AMETHYST("Amethyst", 0xFFCC55FF),
    SAPPHIRE("Sapphire", 0xFF55AAFF),
    AMBER("Amber", 0xFFFFAA00),
    TOPAZ("Topaz", 0xFFFFFF55),
    JADE("Jade", 0xFF55FF55);

    private final String displayName;
    private final int color;

    Crystal(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    public static Optional<Crystal> inMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        for (Crystal crystal : values()) {
            if (lower.contains(crystal.displayName.toLowerCase(Locale.ROOT) + " crystal")) {
                return Optional.of(crystal);
            }
        }
        return Optional.empty();
    }
}
