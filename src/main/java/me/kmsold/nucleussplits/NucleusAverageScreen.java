package me.kmsold.nucleussplits;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class NucleusAverageScreen extends Screen {
    private static final int GOLD = 0xFFFFAA00;
    private static final int GRAY = 0xFFAAAAAA;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Screen parent;
    private final LifetimeStatsStore lifetimeStore;
    private final String profile;
    private int page;
    private int pageSize;
    private List<LifetimeStatsStore.RunEntry> runs = List.of();

    NucleusAverageScreen(Screen parent, LifetimeStatsStore lifetimeStore, String profile, int page) {
        super(Component.translatable("nucleussplits.avg.title"));
        this.parent = parent;
        this.lifetimeStore = lifetimeStore;
        this.profile = JadeProfile.fromKey(profile).key();
        this.page = Math.max(0, page);
    }

    @Override
    protected void init() {
        super.init();
        runs = lifetimeStore.runs(profile);
        pageSize = Math.max(3, Math.min(9, (height - 122) / 22));
        int pages = pageCount();
        page = Math.min(page, pages - 1);

        int listWidth = Math.min(360, width - 32);
        int start = page * pageSize;
        int end = Math.min(runs.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            LifetimeStatsStore.RunEntry run = runs.get(index);
            int y = 58 + (index - start) * 22;
            addRenderableWidget(Button.builder(runLabel(run), button -> {
                NucleusSplits.averageSelectionChanged(run, !run.includedInAverage());
                minecraft.setScreen(new NucleusAverageScreen(parent, lifetimeStore, profile, page));
            }).size(listWidth, 20).pos(width / 2 - listWidth / 2, y).build());
        }

        int navigationY = height - 52;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button ->
                minecraft.setScreen(new NucleusAverageScreen(parent, lifetimeStore, profile, page - 1)))
                .size(40, 20).pos(width / 2 - 92, navigationY).build());
        previous.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button ->
                minecraft.setScreen(new NucleusAverageScreen(parent, lifetimeStore, profile, page + 1)))
                .size(40, 20).pos(width / 2 + 52, navigationY).build());
        next.active = page + 1 < pages;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .size(100, 20).pos(width / 2 - 50, height - 28).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE0101010);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        JadeProfile jadeProfile = JadeProfile.fromKey(profile);
        graphics.centeredText(font, title, width / 2, 12, GOLD);
        graphics.centeredText(font, Component.translatable("nucleussplits.avg.subtitle",
                Component.translatable(jadeProfile.translationKey())), width / 2, 26, GRAY);

        long included = runs.stream().filter(LifetimeStatsStore.RunEntry::includedInAverage).count();
        graphics.centeredText(font, Component.translatable("nucleussplits.avg.summary", included, runs.size()),
                width / 2, 40, 0xFFFFFFFF);
        graphics.centeredText(font, (page + 1) + " / " + pageCount(), width / 2, height - 46, GRAY);
    }

    private Component runLabel(LifetimeStatsStore.RunEntry run) {
        String status = Component.translatable(run.includedInAverage()
                ? "nucleussplits.avg.included" : "nucleussplits.avg.excluded").getString();
        String text = DATE.format(Instant.ofEpochMilli(run.completedAt()))
                + "  •  " + NucleusSplitsHud.format(run.totalTime()) + "  •  " + status;
        return Component.literal(text).withStyle(run.includedInAverage()
                ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private int pageCount() {
        if (runs.isEmpty()) return 1;
        return Math.max(1, (runs.size() + pageSize - 1) / pageSize);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
