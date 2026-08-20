package pl.kiosel.playerlist.util;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.lang.reflect.Method;

public final class Utils {

    Utils() {}

    public static void log(String msg) {
        Bukkit.getServer().getConsoleSender().sendMessage(ColorUtils.color(msg));
    }

    public static boolean isAvailable() {
        try {
            return !net.md_5.bungee.api.ChatColor.class.isEnum();
        } catch (Throwable t) {
            return false;
        }
    }

    public static String fromLegacy(String chat) {
        if (chat == null) {
            chat = "";
        }
        if (isAvailable() && RichTextHolder.PARSE_METHOD != null) {
            try {
                Object result = RichTextHolder.PARSE_METHOD.invoke(null, chat);
                if (result instanceof BaseComponent[]) {
                    return ComponentSerializer.toString((BaseComponent[]) result);
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return ComponentSerializer.toString(TextComponent.fromLegacyText(chat));
    }

    private static final class RichTextHolder {
        private static final Method PARSE_METHOD = findParseMethod();

        private static Method findParseMethod() {
            try {
                Class<?> parser = Class.forName(
						"pl.kiosel.playerlist.util.MiniMessageParser",
                        true,
                        Utils.class.getClassLoader());
                return parser.getMethod("parseFormat", String.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }
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

    public static void sendProtocolMessage() {
        log("&c===================================");
        log("&c PROTOCOL LIB IS NOT INSTALLED");
        log("&c-----------------------------------");
        log("&c AdvancedPlayerList requires");
        log("&c ProtocolLib to be installed!");
        log("&c Please download ProtocolLib");
        log("&c and install it on your server!");
        log("&c and then restart the server.");
        log("&c THIS IS NOT PLUGIN ERROR!");
        log("&c Plugin is now disabled.");
        log("&c===================================");
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
