package pl.kiosel.playerlist.model;

import org.bukkit.Bukkit;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.util.FakePlayer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerBank {

    private final AdvancedPlayerList plugin;
    private final Map<String, FakePlayer> fakePlayers = new ConcurrentHashMap<>();

    public PlayerBank(AdvancedPlayerList plugin) {
        this.plugin = plugin;
    }

    public void clearFakePlayers() {
        fakePlayers.clear();
    }

    public void createFakePlayer(String name) {
        if (name != null && !name.trim().isEmpty()) {
            fakePlayers.putIfAbsent(normalize(name), new FakePlayer(name));
        }
    }

    public FakePlayer deleteFakePlayer(String name) {
        return name == null ? null : fakePlayers.remove(normalize(name));
    }

    public FakePlayer getFakePlayer(String name) {
        return name == null ? null : fakePlayers.get(normalize(name));
    }

    public int getFakePlayerCount() {
        return fakePlayers.size();
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
        if (plugin.getConfigFile().getBoolean("offline-players.enable")
                && plugin.getOfflinePlayerDatabase() != null) {
            players.addAll(plugin.getOfflinePlayerDatabase().getOfflinePlayers());
        }
        return players;
    }

    public void load(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(input);
        fakePlayers.clear();
        int count = data.readInt();
        if (count < 0 || count > 100_000) {
            throw new IOException("Invalid fake player count: " + count);
        }

        for (int index = 0; index < count; index++) {
            String name = data.readUTF();
            FakePlayer player = new FakePlayer(name);
            int placeholders = data.readInt();
            if (placeholders < 0 || placeholders > 100_000) {
                throw new IOException("Invalid placeholder count: " + placeholders);
            }
            for (int placeholder = 0; placeholder < placeholders; placeholder++) {
                player.placeholders().put(data.readUTF(), data.readUTF());
            }
            fakePlayers.put(normalize(name), player);
        }
    }

    public void save(OutputStream output) throws IOException {
        DataOutputStream data = new DataOutputStream(output);
        List<FakePlayer> players = getFakePlayers();
        data.writeInt(players.size());
        for (FakePlayer player : players) {
            data.writeUTF(player.getName());
            Map<String, String> placeholders = new HashMap<>(player.placeholders());
            data.writeInt(placeholders.size());
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeUTF(entry.getValue());
            }
        }
        data.flush();
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
