package me.kmsold.nucleussplits;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class NucleusConfigScreen extends Screen {
    private static final int GOLD = 0xFFFFAA00;
    private final NucleusRunTracker tracker;
    private final ModSettings settings;
    private final LifetimeStatsStore.Snapshot session;
    private final LifetimeStatsStore.Snapshot lifetime;
    private final Screen parent;
    private boolean dragging;
    private double grabX;
    private double grabY;

    NucleusConfigScreen(NucleusRunTracker tracker, ModSettings settings,
                        LifetimeStatsStore.Snapshot session,
                        LifetimeStatsStore.Snapshot lifetime, Screen parent) {
        super(Component.translatable("nucleussplits.gui.title"));
        this.tracker = tracker;
        this.settings = settings;
        this.session = session;
        this.lifetime = lifetime;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clampPosition();
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .size(100, 20).pos(width / 2 - 50, height - 28).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB0101010);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(font, Component.translatable("nucleussplits.gui.title"), width / 2, 10, GOLD);
        graphics.centeredText(font, Component.translatable("nucleussplits.gui.drag"), width / 2, 24, 0xFFDDDDDD);
        graphics.centeredText(font, Component.translatable("nucleussplits.gui.scroll", settings.scalePercent),
                width / 2, 36, 0xFFAAAAAA);
        graphics.centeredText(font, Component.translatable("nucleussplits.gui.reset"), width / 2, 48, 0xFFAAAAAA);

        NucleusSplitsHud.render(graphics, tracker, settings, true, NucleusSplits.now(),
                session, lifetime, false, settings.runProfile);
        if (inside(mouseX, mouseY)) {
            int x2 = settings.x + NucleusSplitsHud.scaledWidth(settings);
            int y2 = settings.y + NucleusSplitsHud.scaledHeight(settings, false);
            graphics.fill(settings.x - 2, settings.y - 2, x2 + 2, settings.y, 0xAAFFFFFF);
            graphics.fill(settings.x - 2, y2, x2 + 2, y2 + 2, 0xAAFFFFFF);
            graphics.fill(settings.x - 2, settings.y, settings.x, y2, 0xAAFFFFFF);
            graphics.fill(x2, settings.y, x2 + 2, y2, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (inside(click.x(), click.y())) {
            if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                dragging = true;
                grabX = click.x() - settings.x;
                grabY = click.y() - settings.y;
                return true;
            }
            if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                settings.x = 8;
                settings.y = 8;
                settings.scalePercent = 100;
                clampPosition();
                SettingsStore.save(settings);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double mouseX, double mouseY) {
        if (dragging && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            settings.x = (int) Math.round(click.x() - grabX);
            settings.y = (int) Math.round(click.y() - grabY);
            clampPosition();
            return true;
        }
        return super.mouseDragged(click, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            SettingsStore.save(settings);
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!inside(mouseX, mouseY) || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        float oldScale = NucleusSplitsHud.scale(settings);
        double relativeX = mouseX - settings.x;
        double relativeY = mouseY - settings.y;
        int step = verticalAmount > 0 ? 5 : -5;
        settings.scalePercent = Math.clamp(settings.scalePercent + step, 50, 200);
        float ratio = NucleusSplitsHud.scale(settings) / oldScale;
        settings.x = (int) Math.round(mouseX - relativeX * ratio);
        settings.y = (int) Math.round(mouseY - relativeY * ratio);
        clampPosition();
        SettingsStore.save(settings);
        return true;
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= settings.x && mouseX < settings.x + NucleusSplitsHud.scaledWidth(settings)
                && mouseY >= settings.y && mouseY < settings.y + NucleusSplitsHud.scaledHeight(settings, false);
    }

    private void clampPosition() {
        settings.x = Math.clamp(settings.x, 0, Math.max(0, width - NucleusSplitsHud.scaledWidth(settings)));
        settings.y = Math.clamp(settings.y, 0, Math.max(0, height - NucleusSplitsHud.scaledHeight(settings, true) - 4));
    }

    @Override
    public void onClose() {
        SettingsStore.save(settings);
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
