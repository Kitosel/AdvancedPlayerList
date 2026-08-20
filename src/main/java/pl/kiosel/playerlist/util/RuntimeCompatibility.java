package pl.kiosel.playerlist.util;

import org.bukkit.Bukkit;
import pl.kiosel.rosacore.utils.NumberUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuntimeCompatibility {

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(?:^|[^0-9])(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private RuntimeCompatibility() {
    }

    public static boolean usesModernPlayerInfo() {
        return isMinecraftAtLeast(1, 19, 3);
    }

    public static boolean isMinecraftAtLeast(int major, int minor, int patch) {
        int[] minecraftVersion = MinecraftVersionHolder.VERSION;
        if (minecraftVersion[0] != major) {
            return minecraftVersion[0] > major;
        }
        if (minecraftVersion[1] != minor) {
            return minecraftVersion[1] > minor;
        }
        return minecraftVersion[2] >= patch;
    }

    public static int getJavaVersion() {
        return JavaVersionHolder.VERSION;
    }

    public static String getMinecraftVersion() {
        int[] minecraftVersion = MinecraftVersionHolder.VERSION;
        return minecraftVersion[0] + "." + minecraftVersion[1] + "." + minecraftVersion[2];
    }

    private static int[] parseMinecraftVersion() {
        Matcher matcher = VERSION_PATTERN.matcher(Bukkit.getBukkitVersion());
        if (!matcher.find()) {
            return new int[]{1, 8, 0};
        }

        return new int[]{
                NumberUtils.parseInt(matcher.group(1), 1),
                NumberUtils.parseInt(matcher.group(2), 8),
                NumberUtils.parseInt(matcher.group(3), 0)
        };
    }

    private static int parseJavaVersion() {
        String version = System.getProperty("java.specification.version", "8");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot >= 0) {
            version = version.substring(0, dot);
        }
        return NumberUtils.parseInt(version, 8);
    }

    private static final class MinecraftVersionHolder {
        private static final int[] VERSION = parseMinecraftVersion();
    }

    private static final class JavaVersionHolder {
        private static final int VERSION = parseJavaVersion();
    }
}
