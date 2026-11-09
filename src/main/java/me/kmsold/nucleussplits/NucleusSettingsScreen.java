package me.kmsold.nucleussplits;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class NucleusSettingsScreen extends Screen {
    private static final int GOLD = 0xFFFFAA00;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int GREEN = 0xFF55FF55;

    private final ModSettings settings;
    private final SessionStatsStore sessionStore;
    private final LifetimeStatsStore lifetimeStore;

    NucleusSettingsScreen(ModSettings settings, SessionStatsStore sessionStore,
                          LifetimeStatsStore lifetimeStore) {
        super(Component.translatable("nucleussplits.menu.title"));
        this.settings = settings;
        this.sessionStore = sessionStore;
        this.lifetimeStore = lifetimeStore;
    }

    @Override
    protected void init() {
        super.init();
        int center = width / 2;
        addRenderableWidget(Button.builder(profileButtonText(), button -> {
            JadeProfile next = JadeProfile.fromKey(settings.runProfile).next();
            settings.runProfile = next.key();
            NucleusSplits.profileChanged();
            minecraft.setScreen(new NucleusSettingsScreen(settings, sessionStore, lifetimeStore));
        }).size(250, 20).pos(center - 125, 64).build());

        addRenderableWidget(Button.builder(viewButtonText(), button -> {
            settings.statsView = settings.statsView.next();
            SettingsStore.save(settings);
            minecraft.setScreen(new NucleusSettingsScreen(settings, sessionStore, lifetimeStore));
        }).size(250, 20).pos(center - 125, 88).build());

        addRenderableWidget(Button.builder(Component.translatable("nucleussplits.menu.avg_runs"), button ->
                minecraft.setScreen(new NucleusAverageScreen(this, lifetimeStore, settings.runProfile, 0)))
                .size(250, 20).pos(center - 125, 112).build());

        addRenderableWidget(Button.builder(Component.translatable("nucleussplits.menu.edit_hud"), button ->
                minecraft.setScreen(new NucleusConfigScreen(
                        NucleusSplits.tracker(), settings,
                        sessionStore.snapshot(settings.runProfile),
                        lifetimeStore.snapshot(settings.runProfile), this)))
                .size(250, 20).pos(center - 125, 176).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .size(100, 20).pos(center - 50, height - 28).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE0101010);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(font, title, width / 2, 18, GOLD);
        graphics.centeredText(font, Component.translatable("nucleussplits.menu.subtitle"),
                width / 2, 34, GRAY);

        LifetimeStatsStore.Snapshot session = sessionStore.snapshot(settings.runProfile);
        LifetimeStatsStore.Snapshot allTime = lifetimeStore.snapshot(settings.runProfile);
        graphics.centeredText(font, Component.translatable("nucleussplits.menu.afk_limit"),
                width / 2, 130, GRAY);
        drawAverage(graphics, "nucleussplits.menu.session_avg", session, 142);
        drawAverage(graphics, "nucleussplits.menu.alltime_avg", allTime, 156);
    }

    private void drawAverage(GuiGraphicsExtractor graphics, String key,
                             LifetimeStatsStore.Snapshot snapshot, int y) {
        Component label = Component.translatable(key);
        String value;
        if (snapshot.runs() == 0) {
            value = Component.translatable("nucleussplits.menu.no_runs").getString();
        } else {
            String average = snapshot.averageRuns() == 0 ? "--:--.--" : NucleusSplitsHud.format(snapshot.averageRun());
            value = average + "  (" + snapshot.averageRuns() + "/" + snapshot.runs() + ")";
        }
        int labelX = width / 2 - 125;
        graphics.text(font, label, labelX, y, WHITE, false);
        graphics.text(font, value, width / 2 + 125 - font.width(value), y,
                snapshot.averageRuns() == 0 ? GRAY : GREEN, false);
    }

    private Component profileButtonText() {
        JadeProfile profile = JadeProfile.fromKey(settings.runProfile);
        return Component.translatable("nucleussplits.menu.jade_mode",
                Component.translatable(profile.translationKey()));
    }

    private Component viewButtonText() {
        String key = settings.statsView == StatsView.SESSION
                ? "nucleussplits.view.session" : "nucleussplits.view.alltime";
        return Component.translatable("nucleussplits.menu.hud_view", Component.translatable(key));
    }

    @Override
    public void onClose() {
        SettingsStore.save(settings);
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
