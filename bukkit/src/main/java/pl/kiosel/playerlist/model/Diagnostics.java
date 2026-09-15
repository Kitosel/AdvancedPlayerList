package pl.kiosel.playerlist.model;

import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Diagnostics {

    private static final int MAX_RECORDED_ERRORS = 20;
    private static final int MAX_MESSAGE_LENGTH = 180;
    private static final Set<String> RUNTIME_ERRORS = new LinkedHashSet<>();

    private Diagnostics() {
    }

    public static List<String> inspect(AdvancedPlayerList plugin) {
        List<String> problems = new ArrayList<>();

        if (plugin.getNMS() == null) {
            problems.add("RosaCore NMS is not initialized");
        }
        if (!AdvancedPlayerList.isPlaceholderAPI()) {
            problems.add("PlaceholderAPI is not enabled");
        }
        if ("none".equalsIgnoreCase(Evaluator.getEngineSource())) {
            problems.add("JavaScript engine is not available");
        }

        if (plugin.getConfigFile() == null || plugin.getHandlerFile() == null || plugin.getGlobalFile() == null) {
            problems.add("One or more configuration files are not loaded");
        } else {
            int minimum = plugin.getConfigFile().getInt("slot-length.min-length");
            int maximum = plugin.getConfigFile().getInt("slot-length.max-length");
            if (minimum < 0 || maximum < 1 || minimum > maximum) {
                problems.add("Invalid slot-length range: " + minimum + "-" + maximum);
            }
            if (plugin.getConfigFile().getInt("task-interval") < 1) {
                problems.add("task-interval must be at least 1");
            }
        }

        if (plugin.globalLayout == null || plugin.globalDisplay == null) {
            problems.add("Global tab-list layout is not loaded");
        }
        if (!plugin.getLayouts().keySet().equals(plugin.getDisplays().keySet())) {
            problems.add("World layouts and displays do not match");
        }
        if (plugin.getTablistManager() == null) {
            problems.add("Tab-list manager is not initialized");
        } else if (plugin.getConfigFile() != null
                && plugin.getConfigFile().getBoolean("tablist-enabled")
                && !plugin.getTablistManager().isTablistEnabled()) {
            problems.add("Tab list is enabled in config but disabled at runtime");
        }
        if (plugin.getPlayerBank() == null) {
            problems.add("Fake-player storage is not initialized");
        }
        if (plugin.getBridgeClient() != null
                && plugin.getBridgeClient().isEnabled()
                && !plugin.getBridgeClient().isConnected()) {
            problems.add("Proxy bridge is enabled but not connected");
        }
        if (plugin.getConfigFile() != null
                && plugin.getConfigFile().getBoolean("offline-players.enable")
                && plugin.getOfflinePlayerDatabase() == null) {
            problems.add("Offline-player storage is not initialized");
        }

        addPlaceholders(problems, "Missing PlaceholderAPI placeholders: ",
                PlaceholderManager.getMissingPlaceholderAPIPlaceholders());
        addPlaceholders(problems, "Unknown native placeholders or handlers: ",
                PlaceholderManager.getMissingNativePlaceholders());

        for (String error : new ArrayList<>(Evaluator.getErrors())) {
            problems.add("Script: " + shorten(error));
        }
        synchronized (RUNTIME_ERRORS) {
            problems.addAll(RUNTIME_ERRORS);
        }
        return problems;
    }

    public static void record(String source, Throwable throwable) {
        String type = throwable == null ? "Unknown error" : throwable.getClass().getSimpleName();
        String message = throwable == null ? "" : shorten(throwable.getMessage());
        String problem = source + ": " + type + (message.isEmpty() ? "" : " - " + message);

        synchronized (RUNTIME_ERRORS) {
            if (!RUNTIME_ERRORS.contains(problem) && RUNTIME_ERRORS.size() >= MAX_RECORDED_ERRORS) {
                Iterator<String> iterator = RUNTIME_ERRORS.iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            RUNTIME_ERRORS.add(problem);
        }
    }

    public static void clear() {
        RUNTIME_ERRORS.clear();
    }

    private static void addPlaceholders(List<String> problems, String prefix, Set<String> placeholders) {
        if (!placeholders.isEmpty()) {
            problems.add(prefix + String.join(", ", placeholders));
        }
    }

    private static String shorten(String message) {
        if (message == null) {
            return "";
        }
        String result = message.trim();
        int lineBreak = result.indexOf('\n');
        if (lineBreak >= 0) {
            result = result.substring(0, lineBreak).trim();
        }
        if (result.length() > MAX_MESSAGE_LENGTH) {
            result = result.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }
        return result;
    }
}
