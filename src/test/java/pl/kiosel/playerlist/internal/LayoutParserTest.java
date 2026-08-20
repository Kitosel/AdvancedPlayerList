package pl.kiosel.playerlist.internal;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import pl.kiosel.playerlist.tablist.TablistDisplay;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutParserTest {

    @Test
    void parsesMultilineHeaderAndFooter() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("header.lines", Arrays.asList("header one", "header two"));
        configuration.set("footer.lines", Arrays.asList("footer one", "footer two"));

        TablistDisplay display = LayoutParser.parseDisplay(
                configuration.getConfigurationSection("header"),
                configuration.getConfigurationSection("footer"));

        assertEquals("header one\nheader two", display.getCurrentHeader());
        assertEquals("footer one\nfooter two", display.getCurrentFooter());
    }

    @Test
    void preservesLegacyTextConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("header.text", "legacy header");
        configuration.set("footer.text", "legacy footer");

        TablistDisplay display = LayoutParser.parseDisplay(
                configuration.getConfigurationSection("header"),
                configuration.getConfigurationSection("footer"));

        assertEquals("legacy header", display.getCurrentHeader());
        assertEquals("legacy footer", display.getCurrentFooter());
    }

    @Test
    void parsesLinesInsideAnimatedFrames() {
        YamlConfiguration configuration = new YamlConfiguration();
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("lines", Arrays.asList("animated one", "animated two"));
        frame.put("delay", 30);
        configuration.set("header.frames", Arrays.asList(frame));

        TablistDisplay display = LayoutParser.parseDisplay(
                configuration.getConfigurationSection("header"), null);

        assertEquals("animated one\nanimated two", display.getCurrentHeader());
        assertEquals("", display.getCurrentFooter());
    }

	@Test
	void bundledGlobalConfigurationContainsAValidMultilineDisplay() {
		InputStream input = LayoutParserTest.class.getResourceAsStream("/global.yml");
		assertNotNull(input);

		YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
				new InputStreamReader(input, StandardCharsets.UTF_8));
		TablistDisplay display = LayoutParser.parseDisplay(
				configuration.getConfigurationSection("header"),
				configuration.getConfigurationSection("footer"));

		assertTrue(display.getCurrentHeader().contains("\n"));
		assertTrue(display.getCurrentFooter().contains("\n"));
	}

    @Test
    void bundledConfigurationFilesAreReadableAndContainTheDefaultHandlers() {
        for (String resource : Arrays.asList("/config.yml", "/global.yml", "/handler.yml")) {
            InputStream input = LayoutParserTest.class.getResourceAsStream(resource);
            assertNotNull(input, resource);
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            assertTrue(!configuration.getKeys(false).isEmpty(), resource);
        }

        InputStream input = LayoutParserTest.class.getResourceAsStream("/handler.yml");
        assertNotNull(input);
        YamlConfiguration handlers = YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        assertEquals("PLAYER_LIST", handlers.getString("players.type"));
        assertEquals("PLAYER_LIST", handlers.getString("worldPlayers.type"));
        assertEquals("COMPOUND", handlers.getString("dynamicSlots.type"));
    }
}
