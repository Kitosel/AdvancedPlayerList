package pl.kiosel.playerlist.util;

import org.bukkit.Bukkit;
import pl.kiosel.rosacore.utils.ColorUtils;

public final class Utils {

    Utils() {}

    private static void log(String msg) {
        Bukkit.getServer().getConsoleSender().sendMessage(ColorUtils.color(msg));
    }

    public static void sendPlaceholderMessage() {
        log("&7===================================");
        log("&e PLACEHOLDERAPI IS NOT INSTALLED");
        log("&7-----------------------------------");
        log("&e AdvancedPlayerList requires the ");
        log("&e PlaceholderAPI for better performance");
        log("&e and to replace placeholders");
        log("&e you can continue using the plugin");
        log("&e but it may cause errors.");
        log("&e Install it and restart the server.");
        log("&c THIS IS NOT PLUGIN ERROR");
        log("&7===================================");
    }

    public static void sendEngineMessage() {
        log("&c===================================");
        log("&c JAVASCRIPT ENGINE IS UNAVAILABLE");
        log("&c-----------------------------------");
        log("&c Java 8 and 11 use their built-in");
        log("&c JavaScript engine automatically.");
        log("&c On newer Java versions install");
        log("&c a compatible JSEngine plugin");
        log("&c and then restart the server.");
        log("&c THIS IS NOT PLUGIN ERROR!");
        log("&c Plugin is now disabled.");
        log("&c===================================");
    }
}
