package pl.kiosel.playerlist.internal;

import org.junit.jupiter.api.Test;
import pl.kiosel.playerlist.tablist.TablistLayout;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalLayoutHandlerTest {

    @Test
    void defaultDisplayFillsOnlyUnconfiguredSlots() {
        TablistLayout layout = new TablistLayout(20, Collections.singletonList("{players}"));
        layout.setDefaultDisplay("fallback");

        new InternalLayoutHandler(null).handle(null, layout);

        assertEquals("{players}", layout.getLine(0).getText());
        assertEquals("fallback", layout.getLine(1).getText());
    }
}
