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
}
