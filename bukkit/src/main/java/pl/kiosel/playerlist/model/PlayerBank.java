package pl.kiosel.playerlist.model;

import org.bukkit.Bukkit;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.ConfigFile;
import pl.kiosel.playerlist.config.Settings;
import pl.kiosel.playerlist.event.FakePlayerCreateEvent;
import pl.kiosel.playerlist.event.FakePlayerRemoveEvent;
import pl.kiosel.playerlist.model.skin.SkinToolkit;
import pl.kiosel.playerlist.util.FakePlayer;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlayerBank {

    private static final int STORAGE_MAGIC = 0x41504C46;
    private static final int LEGACY_FORMAT_2 = -2;
    private static final int CURRENT_FORMAT = 3;

    private final AdvancedPlayerList plugin;
    private final Map<String, FakePlayer> fakePlayers = new ConcurrentHashMap<>();
    private final Set<String> pendingSkinLookups = ConcurrentHashMap.newKeySet();
    private final Set<String> unavailableSkinLookups = ConcurrentHashMap.newKeySet();
    private final String fakePlayersFile;
    private volatile int loadedFormatVersion = CURRENT_FORMAT;

    public PlayerBank(AdvancedPlayerList plugin) {
        this.plugin = plugin;
        this.fakePlayersFile = ConfigFile.FAKE_PLAYER.getPath();
    }

    public void clearFakePlayers() {
        fakePlayers.clear();
        pendingSkinLookups.clear();
        unavailableSkinLookups.clear();
    }

    public void createFakePlayer(String name) {
        createFakePlayer(name, true);
    }

    public void createFakePlayer(String name, boolean renderHead) {
        if (name != null && !name.trim().isEmpty()) {
            FakePlayer fakePlayer = new FakePlayer(name, renderHead);

            FakePlayerCreateEvent event = new FakePlayerCreateEvent(fakePlayer);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            fakePlayers.putIfAbsent(normalize(name), fakePlayer);
        }
    }

    public FakePlayer deleteFakePlayer(String name) {
        if (name == null) {
            return null;
        }
        String key = normalize(name);
        removeLookupState(pendingSkinLookups, key);
        removeLookupState(unavailableSkinLookups, key);

        FakePlayerRemoveEvent event = new FakePlayerRemoveEvent(getFakePlayer(name));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        return fakePlayers.remove(key);
    }

    public FakePlayer getFakePlayer(String name) {
        return name == null ? null : fakePlayers.get(normalize(name));
    }

    public int getFakePlayerCount() {
        return fakePlayers.size();
    }

    public int getPendingSkinLookupCount() {
        return pendingSkinLookups.size();
    }

    public int getUnavailableSkinLookupCount() {
        return unavailableSkinLookups.size();
    }

    public int getStorageFormatVersion() {
        return loadedFormatVersion;
    }

    public List<FakePlayer> getFakePlayers() {
        List<FakePlayer> result = new ArrayList<>(fakePlayers.values());
        result.sort(Comparator.comparing(FakePlayer::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<Object> getOnlinePlayers() {
        ArrayList<Object> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (plugin.getConfigFile().getBoolean("fake-player.enable")) {
            players.addAll(getFakePlayers());
        }
        if (plugin.getSettings().get(Settings.OFFLINE_PLAYERS_ENABLE)
                && plugin.getOfflinePlayerDatabase() != null) {
            players.addAll(plugin.getOfflinePlayerDatabase().getOfflinePlayers());
        }
        return players;
    }

    public void load(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(input);
        fakePlayers.clear();
        pendingSkinLookups.clear();
        unavailableSkinLookups.clear();

        int header = data.readInt();
        int format;
        int count;
        if (header == STORAGE_MAGIC) {
            format = data.readInt();
            if (format != CURRENT_FORMAT) {
                throw new IOException("Unsupported fake-player data format: " + format);
            }
            count = data.readInt();
        } else if (header == LEGACY_FORMAT_2) {
            format = 2;
            count = data.readInt();
        } else {
            format = 1;
            count = header;
        }
        if (count < 0 || count > 100_000) {
            throw new IOException("Invalid fake player count: " + count);
        }

        for (int index = 0; index < count; index++) {
            String name = data.readUTF();
            boolean renderHead = format < 2 || data.readBoolean();
            String skinSource = format >= 3 && data.readBoolean() ? data.readUTF() : null;
            UUID onlineUniqueId = format >= 3 && data.readBoolean() ? new UUID(data.readLong(), data.readLong()) : null;

            FakePlayer player = new FakePlayer(name, renderHead, skinSource, onlineUniqueId);
            int placeholders = data.readInt();
            if (placeholders < 0 || placeholders > 100_000) {
                throw new IOException("Invalid placeholder count: " + placeholders);
            }
            for (int placeholder = 0; placeholder < placeholders; placeholder++) {
                player.placeholders().put(data.readUTF(), data.readUTF());
            }
            fakePlayers.put(normalize(name), player);
        }
        loadedFormatVersion = format;
    }

    public void save(OutputStream output) throws IOException {
        DataOutputStream data = new DataOutputStream(output);
        List<FakePlayer> players = getFakePlayers();

        data.writeInt(STORAGE_MAGIC);
        data.writeInt(CURRENT_FORMAT);
        data.writeInt(players.size());
        for (FakePlayer player : players) {
            data.writeUTF(player.getName());
            data.writeBoolean(player.isRenderHead());
            data.writeBoolean(player.getSkinSource() != null);
            if (player.getSkinSource() != null) {
                data.writeUTF(player.getSkinSource());
            }
            data.writeBoolean(player.getOnlineUniqueId() != null);
            if (player.getOnlineUniqueId() != null) {
                data.writeLong(player.getOnlineUniqueId().getMostSignificantBits());
                data.writeLong(player.getOnlineUniqueId().getLeastSignificantBits());
            }
            Map<String, String> placeholders = new HashMap<>(player.placeholders());
            data.writeInt(placeholders.size());
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeUTF(entry.getValue());
            }
        }
        data.flush();
    }

    public void loadPlayerBank() {
        File file = new File(plugin.getDataFolder(), fakePlayersFile);
        if (!file.isFile())
            return;

        try (InputStream input = Files.newInputStream(file.toPath())) {
            load(input);
        } catch (Throwable throwable) {
            Diagnostics.record("Fake-player data load", throwable);
            plugin.getRosaLogger().log(Level.WARNING, "Unable to load " + fakePlayersFile, throwable);
            return;
        }

        if (loadedFormatVersion < CURRENT_FORMAT) {
            try {
                migrateLegacyFile(file.toPath(), loadedFormatVersion);
            } catch (Throwable throwable) {
                Diagnostics.record("Fake-player data migration", throwable);
                plugin.getRosaLogger().log(Level.WARNING, "Unable to migrate " + fakePlayersFile, throwable);
            }
        }
        for (FakePlayer player : getFakePlayers()) {
            resolveSkin(player, false, null);
        }
    }

    public synchronized void savePlayerBank() {
        Path target = new File(plugin.getDataFolder(), fakePlayersFile).toPath();
        try {
            writePlayerBank(target);
            loadedFormatVersion = CURRENT_FORMAT;
        } catch (Throwable throwable) {
            Diagnostics.record("Fake-player data save", throwable);
            plugin.getRosaLogger().log(Level.WARNING, "Unable to save " + fakePlayersFile, throwable);
        }
    }

    public void resolveSkin(FakePlayer player, boolean force, Consumer<SkinLookupResult> callback) {
        Consumer<SkinLookupResult> result = callback == null ? ignored -> { } : callback;
        if (player == null || !player.isRenderHead()) {
            result.accept(SkinLookupResult.DISABLED);
            return;
        }

        String source = player.getSkinSource();
        if ("none".equalsIgnoreCase(source)) {
            result.accept(SkinLookupResult.DISABLED);
            return;
        }

        SkinToolkit toolkit = SkinToolkit.getDefaultToolkit();
        boolean automatic = source == null;
        String reference = automatic ? player.getName() : source;
        String lookupKey = normalize(player.getName()) + "|" + reference.toLowerCase(Locale.ROOT);
        if (force) {
            toolkit.invalidateSkin(reference);
            if (automatic && player.getOnlineUniqueId() != null) {
                toolkit.invalidateSkin(player.getOnlineUniqueId().toString());
                player.setOnlineUniqueId(null);
            }
        }

        if (automatic && player.getOnlineUniqueId() != null) {
            toolkit.getSkinFromUniqueId(player.getOnlineUniqueId());
            result.accept(SkinLookupResult.REQUESTED);
            return;
        }

        if (reference.length() <= 16) {
            if (!pendingSkinLookups.add(lookupKey)) {
                result.accept(SkinLookupResult.PENDING);
                return;
            }
            toolkit.resolveUniqueId(reference, uuid -> {
                pendingSkinLookups.remove(lookupKey);
                if (uuid == null) {
                    unavailableSkinLookups.add(lookupKey);
                    result.accept(SkinLookupResult.NOT_FOUND);
                    return;
                }
                unavailableSkinLookups.remove(lookupKey);
                if (automatic) {
                    player.setOnlineUniqueId(uuid);
                    savePlayerBank();
                }
                toolkit.getSkinFromUniqueId(uuid);
                result.accept(SkinLookupResult.REQUESTED);
            });
            return;
        }

        if (!toolkit.isValidSkinReference(reference)) {
            result.accept(SkinLookupResult.INVALID);
            return;
        }
        toolkit.getSkinPredicate(reference);
        unavailableSkinLookups.remove(lookupKey);
        result.accept(SkinLookupResult.REQUESTED);
    }

    public void refreshSkin(String name, Consumer<SkinLookupResult> callback) {
        FakePlayer player = getFakePlayer(name);
        if (player == null) {
            if (callback != null) {
                callback.accept(SkinLookupResult.NOT_FOUND);
            }
            return;
        }
        resolveSkin(player, true, callback);
    }

    private void migrateLegacyFile(Path source, int oldVersion) throws IOException {
        Path backup = nextBackupPath(source, oldVersion);
        Files.copy(source, backup);
        writePlayerBank(source);
        loadedFormatVersion = CURRENT_FORMAT;
        plugin.getRosaLogger().info("Migrated " + fakePlayersFile + " from format " + oldVersion
                + " to " + CURRENT_FORMAT + "; backup: " + backup.getFileName());
    }

    private void writePlayerBank(Path target) throws IOException {
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(
                    target.getParent(), target.getFileName().toString() + ".", ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                save(output);
            }
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temporary != null) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private Path nextBackupPath(Path source, int oldVersion) {
        String baseName = source.getFileName().toString() + ".v" + oldVersion + ".bak";
        Path backup = source.resolveSibling(baseName);
        int suffix = 1;
        while (Files.exists(backup)) {
            backup = source.resolveSibling(baseName + "." + suffix++);
        }
        return backup;
    }

    private void removeLookupState(Set<String> values, String playerKey) {
        String prefix = playerKey + "|";
		values.removeIf(value -> value.startsWith(prefix));
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public enum SkinLookupResult {
        DISABLED,
        PENDING,
        REQUESTED,
        NOT_FOUND,
        INVALID
    }
}
