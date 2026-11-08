package me.kmsold.nucleussplits;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

final class PartyPbChat {
    private static final Pattern REQUEST = Pattern.compile(
            "^(?:Party|P)\\s*>\\s*.*?:\\s*!nucpb\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private PartyPbChat() {}

    static boolean isRequest(String strippedChatLine) {
        return REQUEST.matcher(strippedChatLine.trim()).matches();
    }

    static String response(LifetimeStatsStore.Snapshot lifetime) {
        if (lifetime.fullRun() <= 0) return "Nucleus PB: no completed runs yet";
        String runs = lifetime.runs() == 1 ? "1 run" : lifetime.runs() + " runs";
        return String.format(Locale.ROOT, "Nucleus PB: %s (%s)",
                NucleusSplitsHud.format(lifetime.fullRun()), runs);
    }

    /** Stable 0.5-2.5 second spread so party members do not all reply during one tick. */
    static int replyDelayTicks(UUID playerId) {
        return 10 + Math.floorMod(playerId.hashCode(), 41);
    }
}
