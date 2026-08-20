package pl.kiosel.playerlist.placeholder.simple;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.SimplePlaceholder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPISimple implements SimplePlaceholder {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%\\r\\n]+)%");

    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String replace(String string, ExtraData data) {
        if (string == null || string.indexOf('%') < 0) {
            return string;
        }

        Matcher matcher = PLACEHOLDER.matcher(string);
        StringBuffer result = new StringBuffer(string.length());
        while (matcher.find()) {
            String identifier = matcher.group(1);

            // A native placeholder nested inside a PAPI token must be resolved first.
            if (identifier.indexOf('{') >= 0 || identifier.indexOf('}') >= 0) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            boolean viewerPlaceholder = identifier.startsWith("viewer_");
            Object context = data.get(viewerPlaceholder
                    ? ExtraData.DATA_VIEWER
                    : ExtraData.DATA_PLAYER);
            if (!(context instanceof OfflinePlayer)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            String papiIdentifier = viewerPlaceholder ? identifier.substring(7) : identifier;
            String token = "%" + papiIdentifier + "%";
            String replacement = PlaceholderAPI.setPlaceholders((OfflinePlayer) context, token);
            if (replacement == null || token.equals(replacement)) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public boolean repeatAfterParameterizedPlaceholders() {
        return true;
    }
}
