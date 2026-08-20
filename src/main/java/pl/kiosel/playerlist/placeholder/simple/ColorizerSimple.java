package pl.kiosel.playerlist.placeholder.simple;

import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.SimplePlaceholder;
import pl.kiosel.rosacore.utils.ColorUtils;

public class ColorizerSimple implements SimplePlaceholder {

    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String replace(String string, ExtraData data) {
        if (string != null)
            string = ColorUtils.color(string);
        return string;
    }

    @Override
    public boolean repeatAfterParameterizedPlaceholders() {
        return true;
    }
}
