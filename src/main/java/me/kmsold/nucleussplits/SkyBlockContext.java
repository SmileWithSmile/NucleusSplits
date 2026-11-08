package me.kmsold.nucleussplits;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SkyBlockContext {
    private static final Pattern SERVER = Pattern.compile("\\d{2}/\\d{2}/\\d{2}\\s+(\\w+)");

    private SkyBlockContext() {}

    static boolean isSkyBlock(Minecraft client) {
        if (client.getCurrentServer() == null) return false;
        String address = client.getCurrentServer().ip.toLowerCase();
        if (!address.contains("hypixel")) return false;
        return sidebar(client).stream().anyMatch(line -> line.toUpperCase().contains("SKYBLOCK"));
    }

    static String serverKey(Minecraft client) {
        for (String line : sidebar(client)) {
            Matcher matcher = SERVER.matcher(line);
            if (matcher.find()) return matcher.group(1);
        }
        return null;
    }

    private static List<String> sidebar(Minecraft client) {
        if (client.level == null) return List.of();
        Scoreboard scoreboard = client.level.getScoreboard();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return List.of();
        List<String> lines = new ArrayList<>();
        lines.add(objective.getDisplayName().getString());
        scoreboard.listPlayerScores(objective).forEach(entry -> {
            var team = scoreboard.getPlayersTeam(entry.owner());
            lines.add((team == null ? "" : team.getPlayerPrefix().getString())
                    + entry.owner()
                    + (team == null ? "" : team.getPlayerSuffix().getString()));
        });
        return lines;
    }
}
