package pl.kiosel.playerlist.util;

import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FakePlayer {

    public static final Map<String, String> GLOBAL_PLACEHOLDER = new ConcurrentHashMap<>();
    private final Map<String, String> placeholder;
    @Getter
    private final String name;
    private final UUID uuid;
    @Getter
    private final boolean renderHead;
    
    public FakePlayer(String name) {
        this(name, true);
    }

    public FakePlayer(String name, boolean renderHead) {
        this.placeholder = new ConcurrentHashMap<>();
        this.name = name;
        this.uuid = UUID.nameUUIDFromBytes("Offline:".concat(name).getBytes(StandardCharsets.UTF_8));
        this.renderHead = renderHead;
    }

	public UUID getUniqueId() { return uuid; }
    public Map<String, String> placeholders() { return this.placeholder; }

    public String skinIdentifier(String configuredSkin) {
        if (!renderHead) {
            return "";
        }
        if (configuredSkin == null || configuredSkin.trim().isEmpty()
                || configuredSkin.trim().equalsIgnoreCase(uuid.toString())) {
            return name;
        }
        return configuredSkin;
    }
}
