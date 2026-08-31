package pl.kiosel.playerlist.placeholder;

import static pl.kiosel.rosacore.utils.ColorUtils.color;

public interface Placeholder {
    void onRegistered();
    void onUnregistered();

    default String replaceToken(String input, String key, String value) {
        String replacement = color(value);
        return input.replace("{" + key + "}", replacement)
                .replace("%" + key + "%", replacement);
    }
}
