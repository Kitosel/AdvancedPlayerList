package pl.kiosel.playerlist.util;

import lombok.Getter;
import lombok.Setter;

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
    @Getter @Setter
    private volatile boolean renderHead;
    @Getter
    private volatile String skinSource;
    @Getter @Setter
    private volatile UUID onlineUniqueId;
    
    public FakePlayer(String name) {
        this(name, true);
    }

    public FakePlayer(String name, boolean renderHead) {
        this(name, renderHead, null, null);
    }

    public FakePlayer(String name, boolean renderHead, String skinSource, UUID onlineUniqueId) {
        this.placeholder = new ConcurrentHashMap<>();
        this.name = name;
        this.uuid = UUID.nameUUIDFromBytes("Offline:".concat(name).getBytes(StandardCharsets.UTF_8));
        this.renderHead = renderHead;
        setSkinSource(skinSource);
        this.onlineUniqueId = onlineUniqueId;
    }

	public UUID getUniqueId() { return uuid; }
    public Map<String, String> placeholders() { return this.placeholder; }

    public String skinIdentifier(String configuredSkin) {
        if (!renderHead || "none".equalsIgnoreCase(skinSource)) {
            return "";
        }
        if (skinSource != null) {
            return skinSource;
        }
        if (onlineUniqueId != null) {
            return onlineUniqueId.toString();
        }
        if (configuredSkin == null || configuredSkin.trim().isEmpty()
                || configuredSkin.trim().equalsIgnoreCase(uuid.toString())) {
            return name;
        }
        return configuredSkin;
    }

    public void setSkinSource(String skinSource) {
        if (skinSource == null || skinSource.trim().isEmpty()
                || "auto".equalsIgnoreCase(skinSource.trim())) {
            this.skinSource = null;
        } else {
            this.skinSource = skinSource.trim();
        }
    }

    public String describeSkinSource() {
        if (!renderHead) {
            return "disabled";
        }
        if (skinSource != null) {
            return skinSource;
        }
        return onlineUniqueId == null ? "auto (" + name + ")" : "auto (" + onlineUniqueId + ")";
    }
}
