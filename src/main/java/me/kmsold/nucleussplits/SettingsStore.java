package me.kmsold.nucleussplits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nucleus-splits.json");

    private SettingsStore() {}

    public static ModSettings load() {
        if (!Files.exists(FILE)) return new ModSettings();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            ModSettings settings = GSON.fromJson(reader, ModSettings.class);
            if (settings == null) return new ModSettings();
            if (settings.statsView == null) settings.statsView = StatsView.SESSION;
            if (settings.runProfile == null || settings.runProfile.isBlank()) settings.runProfile = "unclassified";
            settings.scalePercent = Math.clamp(settings.scalePercent, 50, 200);
            return settings;
        } catch (Exception exception) {
            NucleusSplits.LOGGER.warn("Could not read {}, using defaults", FILE, exception);
            return new ModSettings();
        }
    }

    public static void save(ModSettings settings) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(settings, writer);
            }
        } catch (IOException exception) {
            NucleusSplits.LOGGER.error("Could not save {}", FILE, exception);
        }
    }
}
