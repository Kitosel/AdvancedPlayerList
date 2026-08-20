package pl.kiosel.playerlist.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayDataTest {

    @Test
    void joinsEveryConfiguredLineWithoutLosingEmptyValues() {
        DisplayData data = new DisplayData(Arrays.asList("first", "", null, "last"), 20);

        assertEquals("first\n\n\nlast", data.getText());
        assertEquals(20, data.getDelay());
    }

    @Test
    void readsLinesDelayAndConditionFromAnimatedFrame() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("lines", Arrays.asList("top", "bottom"));
        frame.put("delay", 60);
        frame.put("condition", "true");

        DisplayData data = new DisplayData(frame, 20);

        assertEquals("top\nbottom", data.getText());
        assertEquals(60, data.getDelay());
        assertEquals("true", data.getCondition());
    }

    @Test
    void usesDefaultDelayWhenFrameDelayIsMissing() {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("text", "legacy");

        DisplayData data = new DisplayData(frame, 15);

        assertEquals("legacy", data.getText());
        assertEquals(15, data.getDelay());
    }

	@Test
	void safelyHandlesMissingContentAndInvalidDefaultDelay() {
		DisplayData data = new DisplayData(null, 0);

		assertEquals("", data.getText());
		assertEquals(1, data.getDelay());
	}
}
