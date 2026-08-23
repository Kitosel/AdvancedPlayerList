package pl.kiosel.playerlist.model;

import org.junit.jupiter.api.Test;
import pl.kiosel.playerlist.model.skin.SkinToolkit;

import static org.junit.jupiter.api.Assertions.assertNull;

class SkinToolkitTest {

    @Test
    void rejectsUnresolvedPlaceholdersBeforeMakingSkinRequests() {
        SkinToolkit toolkit = new SkinToolkit();

        assertNull(toolkit.getSkinPredicate("%player_uuid%"));
        assertNull(toolkit.getSkinPredicate("{fakeplayer_uuid}"));
        assertNull(toolkit.getSkinFromName("%player_uuid%"));
    }
}
