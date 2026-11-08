package me.kmsold.nucleussplits;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public final class NucleusSplits implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Nucleus Splits");
    private static final NucleusRunTracker TRACKER = new NucleusRunTracker();
    private static ModSettings settings;
    private static LifetimeStatsStore lifetimeStore;
    private static LifetimeStatsStore.Snapshot lifetimeComparison = LifetimeStatsStore.Snapshot.empty();
    private static Boolean wasInNucleus;
    private static String serverKey;
    private static int waitingForCrystalNameTicks;
    private static boolean preview;
    private static boolean openGui;
    private static long clientTicks;
    private static long partyPbCooldownUntil;
    private static long partyPbReplyAt = -1;
    private static String pendingPartyPbReply;

    @Override
    public void onInitializeClient() {
        settings = SettingsStore.load();
        lifetimeStore = LifetimeStatsStore.load();
        lifetimeComparison = lifetimeStore.snapshot(settings.runProfile);
        ClientTickEvents.END_CLIENT_TICK.register(NucleusSplits::tick);
        ClientReceiveMessageEvents.ALLOW_GAME.register(NucleusSplits::onGameMessage);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetConnection());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetConnection());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof NucleusConfigScreen) return;
            ScreenMouseEvents.allowMouseClick(screen).register((ignored, click) ->
                    !NucleusSplitsHud.handleViewClick(click.x(), click.y(), click.button(), TRACKER, settings, preview));
            ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, delta) ->
                    NucleusSplitsHud.render(graphics, TRACKER, settings, preview, now(),
                            lifetimeComparison, true));
        });
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.DEMO_TIMER,
                Identifier.fromNamespaceAndPath("nucleussplits", "splits"),
                (graphics, ignored) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.screen != null) return;
                    NucleusSplitsHud.render(graphics, TRACKER, settings, preview, now(),
                            lifetimeComparison, false);
                }
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(commands("nucleussplits"));
            dispatcher.register(commands("ns"));
            dispatcher.register(commands("nc"));
        });
        LOGGER.info("Nucleus Splits ready");
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>
    commands(String root) {
        return ClientCommands.literal(root)
                .executes(context -> { tell("nucleussplits.command.help"); return 1; })
                .then(ClientCommands.literal("gui").executes(context -> {
                    openGui = true;
                    return 1;
                }))
                .then(ClientCommands.literal("reset").executes(context -> {
                    TRACKER.resetLobby();
                    wasInNucleus = null;
                    tell("nucleussplits.command.reset");
                    return 1;
                }))
                .then(ClientCommands.literal("preview").executes(context -> {
                    preview = !preview;
                    tell(preview ? "nucleussplits.command.preview.on" : "nucleussplits.command.preview.off");
                    return 1;
                }))
                .then(ClientCommands.literal("pos")
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer(0, 10_000))
                                .then(ClientCommands.argument("y", IntegerArgumentType.integer(0, 10_000))
                                        .executes(context -> {
                                            settings.x = IntegerArgumentType.getInteger(context, "x");
                                            settings.y = IntegerArgumentType.getInteger(context, "y");
                                            SettingsStore.save(settings);
                                            tell("nucleussplits.command.position", settings.x, settings.y);
                                            return 1;
                                        }))))
                .then(ClientCommands.literal("scale")
                        .then(ClientCommands.argument("percent", IntegerArgumentType.integer(50, 200))
                                .executes(context -> {
                                    settings.scalePercent = IntegerArgumentType.getInteger(context, "percent");
                                    SettingsStore.save(settings);
                                    tell("nucleussplits.command.scale", settings.scalePercent);
                                    return 1;
                                })));
    }

    private static void tick(Minecraft client) {
        clientTicks++;
        if (waitingForCrystalNameTicks > 0) waitingForCrystalNameTicks--;
        sendPendingPartyPb(client);
        if (openGui) {
            openGui = false;
            lifetimeComparison = lifetimeStore.snapshot(settings.runProfile);
            client.setScreen(new NucleusConfigScreen(TRACKER, settings, lifetimeComparison));
        }
        if (client.player == null || client.level == null) return;

        String currentServer = SkyBlockContext.serverKey(client);
        if (currentServer != null && serverKey != null && !currentServer.equals(serverKey)) {
            LOGGER.info("Crystal Hollows server changed: {} -> {}; forgetting learned route", serverKey, currentServer);
            TRACKER.resetLobby();
            wasInNucleus = null;
        }
        if (currentServer != null) serverKey = currentServer;
        if (!SkyBlockContext.isSkyBlock(client)) return;

        boolean inside = NucleusArea.contains(client.player.getX(), client.player.getY(), client.player.getZ());
        if (Boolean.TRUE.equals(wasInNucleus) && !inside) {
            lifetimeComparison = lifetimeStore.snapshot(settings.runProfile);
            TRACKER.onNucleusExit(now());
            LOGGER.info("Nucleus run {} started on area exit", TRACKER.runNumber());
        }
        wasInNucleus = inside;
    }

    private static boolean onGameMessage(Component message, boolean overlay) {
        if (overlay) return true;
        String text = ChatFormatting.stripFormatting(message.getString()).trim();
        String lower = text.toLowerCase(Locale.ROOT);

        if (PartyPbChat.isRequest(text)) schedulePartyPbReply();

        if (lower.contains("crystal found") && lower.contains("/5")) {
            waitingForCrystalNameTicks = 40;
            Crystal.inMessage(text).ifPresent(crystal -> acceptCrystal(crystal));
            return true;
        }

        if (waitingForCrystalNameTicks > 0) {
            Crystal.inMessage(text).ifPresent(crystal -> {
                if (lower.equals((crystal.displayName() + " Crystal").toLowerCase(Locale.ROOT))) {
                    acceptCrystal(crystal);
                    waitingForCrystalNameTicks = 0;
                }
            });
        }

        // SkyHanni and similar chat compactors combine the banner and crystal name.
        if (lower.contains("crystal") && lower.contains("found")) {
            Crystal.inMessage(text).ifPresent(NucleusSplits::acceptCrystal);
        }

        if (lower.contains("pick it up near the nucleus vault")
                || lower.contains("earned a crystal loot bundle")
                || lower.contains("crystal nucleus run complete")) {
            if (TRACKER.onNucleusCompleted(now())) {
                LOGGER.info("Nucleus run {} finished: crystals={} clean={} mismatch={}", TRACKER.runNumber(),
                        TRACKER.observedOrder(), TRACKER.cleanFinish(), TRACKER.routeMismatch());
                lifetimeStore.record(TRACKER, settings.runProfile);
            }
        }
        return true;
    }

    private static void acceptCrystal(Crystal crystal) {
        if (TRACKER.onCrystalFound(crystal, now())) {
            LOGGER.info("Run {} split {} at {}", TRACKER.runNumber(), crystal,
                    NucleusSplitsHud.format(TRACKER.cumulativeTimes().get(crystal)));
        }
    }

    private static void resetConnection() {
        TRACKER.resetLobby();
        wasInNucleus = null;
        serverKey = null;
        waitingForCrystalNameTicks = 0;
        partyPbReplyAt = -1;
        pendingPartyPbReply = null;
        partyPbCooldownUntil = 0;
    }

    private static void schedulePartyPbReply() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || clientTicks < partyPbCooldownUntil || partyPbReplyAt >= 0) return;
        LifetimeStatsStore.Snapshot lifetime = lifetimeStore.snapshot(settings.runProfile);
        pendingPartyPbReply = PartyPbChat.response(lifetime);
        partyPbReplyAt = clientTicks + PartyPbChat.replyDelayTicks(client.player.getUUID());
        partyPbCooldownUntil = clientTicks + 30 * 20L;
        LOGGER.info("Party !nucpb request received; reply scheduled in {} ticks", partyPbReplyAt - clientTicks);
    }

    private static void sendPendingPartyPb(Minecraft client) {
        if (partyPbReplyAt < 0 || clientTicks < partyPbReplyAt) return;
        String reply = pendingPartyPbReply;
        partyPbReplyAt = -1;
        pendingPartyPbReply = null;
        if (reply == null || client.getConnection() == null) return;
        client.getConnection().sendCommand("pc " + reply);
        LOGGER.info("Sent party Nucleus PB response: {}", reply);
    }

    static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    private static void tell(String key, Object... args) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.literal("[Nucleus Splits] ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.YELLOW)));
    }
}
