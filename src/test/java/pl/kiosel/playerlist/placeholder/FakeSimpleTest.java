package pl.kiosel.playerlist.placeholder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.kiosel.playerlist.placeholder.simple.FakeSimple;
import pl.kiosel.playerlist.util.FakePlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeSimpleTest {

    @AfterEach
    void clearDefaults() {
        FakePlayer.GLOBAL_PLACEHOLDER.clear();
    }

    @Test
    void resolvesGlobalPapiTokensForFakePlayers() {
        FakePlayer fakePlayer = new FakePlayer("TestBot");
        FakePlayer.GLOBAL_PLACEHOLDER.put("player_name", "{fakeplayer_name}");
        FakePlayer.GLOBAL_PLACEHOLDER.put("player_uuid", "{fakeplayer_uuid}");
        FakePlayer.GLOBAL_PLACEHOLDER.put("player_ping", "0");

        String result = new FakeSimple().replace(
                "%player_name%|%player_uuid%|%player_ping%",
                new ExtraData().put(ExtraData.DATA_PLAYER, fakePlayer));

        assertEquals("TestBot|" + fakePlayer.getUniqueId() + "|0", result);
    }

    @Test
    void playerSpecificValueOverridesGlobalDefaultInBothSyntaxes() {
        FakePlayer fakePlayer = new FakePlayer("TestBot");
        FakePlayer.GLOBAL_PLACEHOLDER.put("player_name", "{fakeplayer_name}");
        fakePlayer.placeholders().put("player_name", "Custom name");

        String result = new FakeSimple().replace(
                "%player_name%|{player_name}",
                new ExtraData().put(ExtraData.DATA_PLAYER, fakePlayer));

        assertEquals("Custom name|Custom name", result);
        assertTrue(new FakeSimple().repeatAfterParameterizedPlaceholders());
    }
}
