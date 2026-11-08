package me.kmsold.nucleussplits;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartyPbChatTest {
    @Test
    void acceptsOnlyExactPartyRequests() {
        assertTrue(PartyPbChat.isRequest("Party > [MVP+] SomePlayer: !nucpb"));
        assertTrue(PartyPbChat.isRequest("P > Player:   !NUCPB  "));
        assertFalse(PartyPbChat.isRequest("Guild > Player: !nucpb"));
        assertFalse(PartyPbChat.isRequest("From Player: !nucpb"));
        assertFalse(PartyPbChat.isRequest("Party > Player: can somebody type !nucpb"));
        assertFalse(PartyPbChat.isRequest("!nucpb"));
    }

    @Test
    void formatsLifetimePbAndEmptyHistory() {
        var pb = new LifetimeStatsStore.Snapshot(Map.of(), 0, 207_650, 215_000, 12);
        assertEquals("Nucleus PB: 03:27.65 (12 runs)", PartyPbChat.response(pb));
        assertEquals("Nucleus PB: no completed runs yet", PartyPbChat.response(LifetimeStatsStore.Snapshot.empty()));
    }

    @Test
    void replyDelayIsStableAndBounded() {
        UUID id = UUID.fromString("12345678-1234-5678-1234-567812345678");
        int delay = PartyPbChat.replyDelayTicks(id);
        assertEquals(delay, PartyPbChat.replyDelayTicks(id));
        assertTrue(delay >= 10 && delay <= 50);
    }
}
