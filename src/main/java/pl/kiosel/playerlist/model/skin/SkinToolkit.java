package pl.kiosel.playerlist.model.skin;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.rosacore.RosaLogger;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SkinToolkit {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    private static final String USER_AGENT = "AdvancedPlayerList-Plugin";

    private static volatile SkinToolkit toolkit = new SkinToolkit();

    private final Map<String, AtomicReference<UUID>> nameToUuid = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicReference<Skin>> uuidToSkin = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<Skin>> dataToSkin = new ConcurrentHashMap<>();
    private volatile String mineSkinApiKey = "";

    public static SkinToolkit getDefaultToolkit() {
        return toolkit;
    }

    public static void setDefaultToolkit(SkinToolkit replacement) {
        if (replacement == null) {
            throw new NullPointerException("toolkit");
        }
        toolkit = replacement;
    }

    public void setMineSkinApiKey(String apiKey) {
        mineSkinApiKey = apiKey == null ? "" : apiKey.trim();
    }

    protected void asyncRequest(String url, Consumer<byte[]> callback) {
        asyncRequest(url, "GET", callback);
    }

    private void asyncRequest(String url, String method, Consumer<byte[]> callback) {
        Ticker.submit(() -> {
            try {
                callback.accept(readUrl(url, method));
            } catch (Throwable throwable) {
                logRequestFailure(url, throwable);
            }
        });
    }

    private byte[] readUrl(String address, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (address.startsWith("https://api.mineskin.org/") && !mineSkinApiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + mineSkinApiKey);
        }
        connection.setRequestMethod(method);

        try {
            int responseCode = connection.getResponseCode();
            InputStream response = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (response == null) {
                throw new IOException("HTTP " + responseCode);
            }

            try (InputStream input = new BufferedInputStream(response);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8_192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("HTTP " + responseCode + ": "
                            + new String(output.toByteArray(), StandardCharsets.UTF_8));
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private void logRequestFailure(String address, Throwable throwable) {
        RosaLogger.getInstance().log(
                Level.WARNING,
                "Unable to download skin data from " + address,
                throwable);
    }

    public void clearCache() {
        nameToUuid.clear();
        uuidToSkin.clear();
        dataToSkin.clear();
    }

    protected UUID fixUniqueId(String value) {
        if (value == null) {
            return new UUID(0L, 0L);
        }
        try {
            if (value.length() == 32 && value.indexOf('-') < 0) {
                value = value.substring(0, 8) + '-'
                        + value.substring(8, 12) + '-'
                        + value.substring(12, 16) + '-'
                        + value.substring(16, 20) + '-'
                        + value.substring(20);
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return new UUID(0L, 0L);
        }
    }

    public Skin getSkinFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return getOnlineSkin(online, null);
        }

        String cacheKey = name.toLowerCase(Locale.ROOT);
        AtomicReference<UUID> existing = nameToUuid.get(cacheKey);
        if (existing != null) {
            UUID uuid = existing.get();
            return uuid == null ? null : getSkinFromUniqueId(uuid, null, false);
        }

        AtomicReference<UUID> pending = new AtomicReference<>();
        AtomicReference<UUID> raced = nameToUuid.putIfAbsent(cacheKey, pending);
        if (raced != null) {
            UUID uuid = raced.get();
            return uuid == null ? null : getSkinFromUniqueId(uuid, null, false);
        }

        Skin skin = new Skin();
        skin.setKey(cacheKey);
        requestAccount(name, account -> {
            if (account.valid && account.uuid != null && !account.uuid.equals(new UUID(0L, 0L))) {
                pending.set(account.uuid);
                getSkinFromUniqueId(account.uuid, skin, false);
            } else {
                nameToUuid.remove(cacheKey, pending);
            }
        });
        return skin;
    }

    private Skin getOnlineSkin(Player player, Skin supplied) {
        for (MetadataValue value : player.getMetadata("advancedplayerlist.skin")) {
            if (value.getOwningPlugin() == AdvancedPlayerList.getInstance()
                    && value.value() instanceof Skin) {
                return (Skin) value.value();
            }
        }

        Collection<WrappedSignedProperty> textures = WrappedGameProfile.fromPlayer(player)
                .getProperties().get("textures");
        if (textures.isEmpty()) {
            return null;
        }

        WrappedSignedProperty property = textures.iterator().next();
        Skin skin = supplied == null ? new Skin() : supplied;
        skin.setKey(player.getUniqueId());
        skin.uuid = player.getUniqueId();
        skin.texture = new SkinTexture();
        skin.texture.signature = property.getSignature();
        skin.texture.value = property.getValue();
        player.setMetadata(
                "advancedplayerlist.skin",
                new FixedMetadataValue(AdvancedPlayerList.getInstance(), skin));
        return skin;
    }

    public Skin getSkinFromUniqueId(UUID uuid) {
        return getSkinFromUniqueId(uuid, null, true);
    }

    public Skin getSkinFromUniqueId(UUID uuid, Skin supplied, boolean checkPlayer) {
        if (uuid == null || uuid.version() == 3) {
            return null;
        }

        if (checkPlayer) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                return getOnlineSkin(online, supplied);
            }
        }

        AtomicReference<Skin> existing = uuidToSkin.get(uuid);
        if (existing != null) {
            return existing.get();
        }

        Skin skin = supplied == null ? new Skin() : supplied;
        skin.setKey(uuid);
        AtomicReference<Skin> pending = new AtomicReference<>(skin);
        AtomicReference<Skin> raced = uuidToSkin.putIfAbsent(uuid, pending);
        if (raced != null) {
            return raced.get();
        }

        requestSkin(uuid.toString().replace("-", ""), skin);
        return supplied == null ? null : skin;
    }

    public Skin getSkinPredicate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() <= 16) {
            return getSkinFromName(value);
        }
        if (value.length() > 36) {
            return getSkinFromURL(value);
        }
        return getSkinFromUniqueId(fixUniqueId(value));
    }

    public Skin getSkinFromURL(String url) {
        AtomicReference<Skin> existing = dataToSkin.get(url);
        if (existing != null) {
            return existing.get();
        }

        Skin skin = new Skin();
        skin.setKey(url);
        AtomicReference<Skin> pending = new AtomicReference<>(skin);
        AtomicReference<Skin> raced = dataToSkin.putIfAbsent(url, pending);
        if (raced != null) {
            return raced.get();
        }

        try {
            String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.name());
            asyncRequest("https://api.mineskin.org/generate/url?url=" + encodedUrl + "&v2=true", "POST",
                    bytes -> acceptJson(new String(bytes, StandardCharsets.UTF_8), skin));
        } catch (IOException exception) {
            dataToSkin.remove(url, pending);
            logRequestFailure(url, exception);
        }
        return skin;
    }

    protected void requestAccount(String name, Consumer<MinecraftAccount> callback) {
        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            Ticker.submit(() -> {
                MinecraftAccount account = new MinecraftAccount();
                account.name = name;
                try {
                    byte[] bytes = readUrl(
                            "https://api.mojang.com/users/profiles/minecraft/" + encodedName,
                            "GET");
                    JSONObject object = new JSONObject(
                            new JSONTokener(new ByteArrayInputStream(bytes)));
                    account.uuid = fixUniqueId(object.optString("id", null));
                    account.name = object.optString("name", name);
                    account.valid = account.uuid != null && !account.uuid.equals(new UUID(0L, 0L));
                } catch (Throwable exception) {
                    logRequestFailure("account " + name, exception);
                }
                callback.accept(account);
            });
        } catch (IOException exception) {
            logRequestFailure("account " + name, exception);
        }
    }

    protected void requestSkin(String accountId, Skin skin) {
        asyncRequest("https://sessionserver.mojang.com/session/minecraft/profile/"
                + accountId + "?unsigned=false", bytes -> {
            try {
                acceptMojangProfile(new String(bytes, StandardCharsets.UTF_8), skin);
            } catch (RuntimeException exception) {
                logRequestFailure("account " + accountId, exception);
            }
        });
    }

    private void acceptMojangProfile(String json, Skin skin) {
        JSONObject profile = new JSONObject(new JSONTokener(json));
        skin.uuid = fixUniqueId(profile.optString("id", null));
        JSONArray properties = profile.getJSONArray("properties");
        for (int index = 0; index < properties.length(); index++) {
            JSONObject property = properties.getJSONObject(index);
            if (!"textures".equals(property.optString("name"))) {
                continue;
            }

            SkinTexture texture = new SkinTexture();
            texture.value = property.getString("value");
            texture.signature = property.optString("signature", null);
            texture.url = extractTextureUrl(texture.value);
            skin.texture = texture;
            return;
        }
        throw new IllegalArgumentException("Mojang profile does not contain a textures property");
    }

    private URL extractTextureUrl(String encodedTexture) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedTexture);
            JSONObject payload = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            String url = payload.getJSONObject("textures")
                    .getJSONObject("SKIN")
                    .getString("url");
            return new URL(url);
        } catch (RuntimeException | MalformedURLException ignored) {
            return null;
        }
    }

    protected void acceptJson(String json, Skin skin) {
        JSONObject root = new JSONObject(new JSONTokener(json));
        JSONObject data;
        JSONObject textureJson;
        JSONObject textureData;
        String textureUrl;

        if (root.has("skin")) {
            data = root.getJSONObject("skin");
            textureJson = data.getJSONObject("texture");
            textureData = textureJson.getJSONObject("data");
            JSONObject urls = textureJson.optJSONObject("url");
            textureUrl = urls == null ? null : urls.optString("skin", null);
        } else {
            data = root.getJSONObject("data");
            textureJson = data.getJSONObject("texture");
            textureData = textureJson;
            textureUrl = textureJson.optString("url", null);
        }

        skin.uuid = fixUniqueId(data.getString("uuid"));
        SkinTexture texture = new SkinTexture();
        texture.value = textureData.getString("value");
        texture.signature = textureData.getString("signature");

        if (textureUrl != null) {
            try {
                texture.url = new URL(textureUrl);
            } catch (MalformedURLException ignored) {
            }
        }
        skin.texture = texture;
    }

    @SuppressWarnings("unchecked")
    public void loadCache(InputStream inputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(inputStream);
        nameToUuid.putAll((Map<? extends String, ? extends AtomicReference<UUID>>) input.readObject());
        uuidToSkin.putAll((Map<? extends UUID, ? extends AtomicReference<Skin>>) input.readObject());
        dataToSkin.putAll((Map<? extends String, ? extends AtomicReference<Skin>>) input.readObject());
        uuidToSkin.forEach((uuid, reference) -> {
            Skin skin = reference.get();
            if (skin != null) {
                skin.setKey(uuid);
            }
        });
        dataToSkin.forEach((key, reference) -> {
            Skin skin = reference.get();
            if (skin != null) {
                skin.setKey(key);
            }
        });
        RosaLogger.getInstance().info("[SkinToolkit] Loaded " + (uuidToSkin.size() + dataToSkin.size()) + " skins");
    }

    public void saveCache(OutputStream outputStream) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(outputStream);
        output.writeObject(nameToUuid);
        output.writeObject(uuidToSkin);
        output.writeObject(dataToSkin);
        output.flush();
        RosaLogger.getInstance().info("[SkinToolkit] Saved " + nameToUuid.size() + " UUIDs");
        RosaLogger.getInstance().info("[SkinToolkit] Saved " + (uuidToSkin.size() + dataToSkin.size()) + " skins");
    }
}
