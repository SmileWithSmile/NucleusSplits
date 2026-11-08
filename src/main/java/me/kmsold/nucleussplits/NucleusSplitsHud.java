package me.kmsold.nucleussplits;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class NucleusSplitsHud {
    static final int BASE_WIDTH = 188;
    static final int BASE_HEIGHT = 101;
    static final int TOGGLE_GAP = 3;
    static final int TOGGLE_HEIGHT = 14;

    private static final int GOLD = 0xFFFFAA00;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK_GRAY = 0xFF555555;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    private NucleusSplitsHud() {}

    static void render(GuiGraphicsExtractor graphics, NucleusRunTracker tracker, ModSettings settings,
                       boolean preview, long nowMillis, LifetimeStatsStore.Snapshot lifetime,
                       boolean showToggle) {
        if (!isVisible(tracker, preview)) return;

        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        float scale = scale(settings);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(settings.x, settings.y);
        pose.scale(scale, scale);

        List<Crystal> order = displayOrder(tracker, preview);
        graphics.fill(0, 0, BASE_WIDTH, BASE_HEIGHT, 0xCC101010);
        border(graphics, 0, 0, BASE_WIDTH, BASE_HEIGHT, GOLD);

        graphics.text(font, Component.literal("Nucleus Splits"), 5, 4, GOLD, true);
        if (!preview && settings.statsView == StatsView.LIFETIME && lifetime.fullRun() > 0) {
            String pb = "PB " + format(lifetime.fullRun());
            graphics.text(font, pb, BASE_WIDTH - 5 - font.width(pb), 4, GREEN, true);
        }
        graphics.text(font, status(tracker, preview, settings.statsView), 5, 15, GRAY, false);

        int y = 27;
        int observedCount = tracker.observedOrder().size();
        for (int i = 0; i < 5; i++) {
            if (i < order.size()) {
                Crystal crystal = order.get(i);
                drawCrystalRow(graphics, font, tracker, crystal, i, observedCount, y, nowMillis,
                        preview, settings.statsView, lifetime);
            } else {
                graphics.text(font, "Crystal " + (i + 1), 5, y, GRAY, false);
                graphics.text(font, "--", 105, y, GRAY, false);
                graphics.text(font, "--", 143, y, GRAY, false);
            }
            y += 10;
        }

        drawNucleusRow(graphics, font, tracker, y, nowMillis, preview, settings.statsView, lifetime);
        y += 11;
        int timerColor = tracker.state() == NucleusRunTracker.State.FINISHED
                ? (tracker.cleanFinish() && !tracker.routeMismatch() ? GREEN : RED) : YELLOW;
        graphics.text(font, format(tracker.elapsed(nowMillis)), 5, y, timerColor, true);

        if (showToggle) drawViewToggle(graphics, font, settings.statsView);
        pose.popMatrix();
    }

    private static void drawViewToggle(GuiGraphicsExtractor graphics, Font font, StatsView view) {
        int y = BASE_HEIGHT + TOGGLE_GAP;
        int half = BASE_WIDTH / 2;
        graphics.fill(0, y, BASE_WIDTH, y + TOGGLE_HEIGHT, 0xDD101010);
        border(graphics, 0, y, BASE_WIDTH, TOGGLE_HEIGHT, GOLD);
        graphics.fill(half, y + 1, half + 1, y + TOGGLE_HEIGHT - 1, DARK_GRAY);
        if (view == StatsView.SESSION) graphics.fill(1, y + 1, half, y + TOGGLE_HEIGHT - 1, 0x5544FF44);
        else graphics.fill(half + 1, y + 1, BASE_WIDTH - 1, y + TOGGLE_HEIGHT - 1, 0x5544FF44);
        graphics.centeredText(font, "SESSION", half / 2, y + 3, view == StatsView.SESSION ? GREEN : GRAY);
        graphics.centeredText(font, "LIFETIME", half + half / 2, y + 3, view == StatsView.LIFETIME ? GREEN : GRAY);
    }

    static boolean handleViewClick(double mouseX, double mouseY, int button, NucleusRunTracker tracker,
                                   ModSettings settings, boolean preview) {
        if (button != 0 || !isVisible(tracker, preview)) return false;
        float scale = scale(settings);
        double localX = (mouseX - settings.x) / scale;
        double localY = (mouseY - settings.y) / scale;
        int top = BASE_HEIGHT + TOGGLE_GAP;
        if (localX < 0 || localX >= BASE_WIDTH || localY < top || localY >= top + TOGGLE_HEIGHT) return false;
        settings.statsView = localX < BASE_WIDTH / 2.0 ? StatsView.SESSION : StatsView.LIFETIME;
        SettingsStore.save(settings);
        return true;
    }

    static boolean isVisible(NucleusRunTracker tracker, boolean preview) {
        return preview || tracker.state() != NucleusRunTracker.State.WAITING_FOR_EXIT
                || !tracker.referenceOrder().isEmpty();
    }

    static float scale(ModSettings settings) {
        return Math.clamp(settings.scalePercent / 100f, 0.5f, 2.0f);
    }

    static int scaledWidth(ModSettings settings) {
        return Math.round(BASE_WIDTH * scale(settings));
    }

    static int scaledHeight(ModSettings settings, boolean withToggle) {
        int base = BASE_HEIGHT + (withToggle ? TOGGLE_GAP + TOGGLE_HEIGHT : 0);
        return Math.round(base * scale(settings));
    }

    private static void border(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static List<Crystal> displayOrder(NucleusRunTracker tracker, boolean preview) {
        if (!tracker.referenceOrder().isEmpty()) return tracker.referenceOrder();
        List<Crystal> result = new ArrayList<>(tracker.observedOrder());
        if (preview && result.isEmpty()) {
            result.add(Crystal.AMETHYST);
            result.add(Crystal.SAPPHIRE);
            result.add(Crystal.AMBER);
            result.add(Crystal.TOPAZ);
            result.add(Crystal.JADE);
        }
        return result;
    }

    private static String status(NucleusRunTracker tracker, boolean preview, StatsView view) {
        String mode = view == StatsView.SESSION ? "session" : "lifetime";
        if (preview && tracker.state() == NucleusRunTracker.State.WAITING_FOR_EXIT) return "Preview • " + mode;
        if (tracker.referenceOrder().isEmpty()) return "Run " + tracker.runNumber() + " • learning • " + mode;
        if (tracker.routeMismatch()) return "Run " + tracker.runNumber() + " • order changed • " + mode;
        return "Run " + tracker.runNumber() + " • " + mode;
    }

    private static long best(NucleusRunTracker tracker, Crystal crystal, StatsView view,
                             LifetimeStatsStore.Snapshot lifetime) {
        return view == StatsView.SESSION ? tracker.comparisonSegment(crystal) : lifetime.segment(crystal);
    }

    private static long nucleusBest(NucleusRunTracker tracker, StatsView view,
                                    LifetimeStatsStore.Snapshot lifetime) {
        return view == StatsView.SESSION ? tracker.comparisonNucleusSegment() : lifetime.nucleusSegment();
    }

    private static void drawCrystalRow(GuiGraphicsExtractor graphics, Font font, NucleusRunTracker tracker,
                                       Crystal crystal, int index, int observedCount, int y, long now, boolean preview,
                                       StatsView view, LifetimeStatsStore.Snapshot lifetime) {
        boolean completed = tracker.cumulativeTimes().containsKey(crystal);
        graphics.text(font, crystal.displayName(), 5, y, crystal.color(), true);
        if (preview && tracker.state() == NucleusRunTracker.State.WAITING_FOR_EXIT) {
            graphics.text(font, index < 2 ? (index == 0 ? "-1.24s" : "+0.35s") : "--", 105, y,
                    index == 1 ? RED : GREEN, false);
            graphics.text(font, format((index + 1L) * 37_250L), 136, y, YELLOW, false);
            return;
        }
        long best = best(tracker, crystal, view, lifetime);
        if (completed) {
            long segment = tracker.segmentTime(crystal, now);
            String middle = best > 0 ? signed(segment - best) : "learn";
            int middleColor = best <= 0 ? GRAY : (segment <= best ? GREEN : RED);
            graphics.text(font, middle, 105, y, middleColor, false);
            graphics.text(font, format(tracker.cumulativeTimes().get(crystal)), 136, y, YELLOW, false);
        } else if (index == observedCount && tracker.state() == NucleusRunTracker.State.RUNNING) {
            graphics.text(font, formatShort(tracker.currentSegmentTime(now)), 105, y, WHITE, false);
            graphics.text(font, best > 0 ? formatShort(best) : "--", 143, y, GRAY, false);
        } else {
            graphics.text(font, "--", 105, y, GRAY, false);
            graphics.text(font, best > 0 ? formatShort(best) : "--", 143, y, GRAY, false);
        }
    }

    private static void drawNucleusRow(GuiGraphicsExtractor graphics, Font font, NucleusRunTracker tracker,
                                       int y, long now, boolean preview, StatsView view,
                                       LifetimeStatsStore.Snapshot lifetime) {
        graphics.text(font, "Nucleus", 5, y, GOLD, true);
        if (preview && tracker.state() == NucleusRunTracker.State.WAITING_FOR_EXIT) {
            graphics.text(font, "-0.52s", 105, y, GREEN, false);
            graphics.text(font, "03:27.50", 136, y, YELLOW, false);
            return;
        }
        long best = nucleusBest(tracker, view, lifetime);
        if (tracker.state() == NucleusRunTracker.State.FINISHED) {
            long segment = tracker.nucleusSegment();
            graphics.text(font, best > 0 ? signed(segment - best) : "learn", 105, y,
                    best <= 0 ? GRAY : (segment <= best ? GREEN : RED), false);
            graphics.text(font, format(tracker.elapsed(now)), 136, y, YELLOW, false);
        } else if (tracker.observedOrder().size() == 5 && tracker.state() == NucleusRunTracker.State.RUNNING) {
            graphics.text(font, formatShort(tracker.currentSegmentTime(now)), 105, y, WHITE, false);
            graphics.text(font, best > 0 ? formatShort(best) : "--", 143, y, GRAY, false);
        } else {
            graphics.text(font, "--", 105, y, GRAY, false);
            graphics.text(font, best > 0 ? formatShort(best) : "--", 143, y, GRAY, false);
        }
    }

    static String format(long millis) {
        long safe = Math.max(0, millis);
        long minutes = safe / 60_000;
        long seconds = (safe / 1_000) % 60;
        long hundredths = (safe % 1_000) / 10;
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    private static String formatShort(long millis) {
        return String.format(Locale.ROOT, "%.2fs", Math.max(0, millis) / 1000.0);
    }

    private static String signed(long millis) {
        return String.format(Locale.ROOT, "%+.2fs", millis / 1000.0);
    }
}
