package pl.kiosel.playerlist.model;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.rosacore.RosaLogger;
import pl.kiosel.rosacore.utils.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OfflinePlayerDatabase {

    public static final String SUFFIX = ".offplayer";

    private static final long DEFAULT_PURGE_TIME = 30L * 24L * 60L * 60L * 1000L;
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms]?)");

    private final Map<UUID, OfflinePlayer> offlinePlayers = new LinkedHashMap<>();
    private long maxTimePurging = DEFAULT_PURGE_TIME;

    public void setMaxTimePurging(String value) {
        if (value == null || value.trim().isEmpty()) {
            maxTimePurging = DEFAULT_PURGE_TIME;
            return;
        }

        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        Matcher matcher = DURATION_PART.matcher(normalized);
        long duration = 0L;
        int parsedUntil = 0;
        while (matcher.find()) {
            if (matcher.start() != parsedUntil) {
                throw new IllegalArgumentException("Invalid purge time: " + value);
            }
            long amount = NumberUtils.parseLongOrZero(matcher.group(1));
            duration = Math.addExact(duration, Math.multiplyExact(amount, multiplier(matcher.group(2))));
            parsedUntil = matcher.end();
        }
        if (parsedUntil != normalized.length() || duration < 0L) {
            throw new IllegalArgumentException("Invalid purge time: " + value);
        }
        maxTimePurging = duration;
    }

    private static long multiplier(String unit) {
        if ("d".equals(unit)) {
            return 24L * 60L * 60L * 1000L;
        }
        if ("h".equals(unit)) {
            return 60L * 60L * 1000L;
        }
        if ("m".equals(unit)) {
            return 60L * 1000L;
        }
        if ("s".equals(unit)) {
            return 1000L;
        }
        return 1L;
    }

    public void initialize() {
        offlinePlayers.clear();
        File directory = getDirectory();
        createDirectory(directory);

        File[] files = directory.listFiles((ignored, name) -> name.endsWith(SUFFIX));
        if (files == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - SUFFIX.length());
            try {
                UUID uuid = UUID.fromString(name);
                if (now - file.lastModified() >= maxTimePurging) {
                    deleteCacheFile(file);
                    continue;
                }

                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (!isValid(player, uuid)) {
                    deleteCacheFile(file);
                    continue;
                }
                offlinePlayers.put(uuid, player);
            } catch (RuntimeException exception) {
                RosaLogger.getInstance().log(
                        Level.WARNING,
                        "Unable to read offline player cache " + file.getName(),
                        exception);
            }
        }
    }

    private boolean isValid(OfflinePlayer player, UUID uuid) {
        String playerName = player.getName();
        if (playerName == null) {
            return false;
        }
        if (Bukkit.getOnlineMode()) {
            return true;
        }
        UUID offlineId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        return offlineId.equals(uuid);
    }

    public void insertBukkit() {
        List<UUID> ids = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            offlinePlayers.put(player.getUniqueId(), player);
            ids.add(player.getUniqueId());
        }
        writeCacheFiles(ids);
    }

    public void add(OfflinePlayer player) {
        if (player == null) {
            return;
        }
        offlinePlayers.put(player.getUniqueId(), player);
        writeCacheFiles(Collections.singletonList(player.getUniqueId()));
    }

    private void writeCacheFiles(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        File directory = getDirectory();
        Ticker.submit(() -> {
            createDirectory(directory);
            for (UUID id : ids) {
                File file = new File(directory, id + SUFFIX);
                try {
                    if (file.createNewFile()) {
                        AdvancedPlayerList.getInstance().getDebug().debug("Created " + file.getName());
                    }
                } catch (IOException exception) {
                    RosaLogger.getInstance().log(Level.WARNING,
                            "Unable to create " + file.getName(),
                            exception);
                    AdvancedPlayerList.getInstance().getDebug().debug("Unable to create " + file.getName(), exception);
                }
            }
        });
    }

    private File getDirectory() {
        return new File(AdvancedPlayerList.getInstance().getDataFolder(), "offlinePlayers");
    }

    private static void createDirectory(File directory) {
        if (!directory.isDirectory() && directory.mkdirs()) {
            AdvancedPlayerList.getInstance().getDebug().debug("Created " + directory.getName() + " directory");
        }
    }

    private static void deleteCacheFile(File file) {
        if (file.delete()) {
            RosaLogger.getInstance().info("Deleted expired cache " + file.getName());
        }
    }

    public void remove(UUID uuid) {
        offlinePlayers.remove(uuid);
    }

    public Collection<OfflinePlayer> getOfflinePlayers() {
        List<OfflinePlayer> result = new ArrayList<>();
        for (OfflinePlayer player : offlinePlayers.values()) {
            String name = player.getName();
            if (!player.isOnline() && (name == null || Bukkit.getPlayer(name) == null)) {
                result.add(player);
            }
        }
        return result;
    }
}
