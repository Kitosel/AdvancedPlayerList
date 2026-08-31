package pl.kiosel.playerlist.placeholder.simple;

import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.SimplePlaceholder;

public class OverflowSimple implements SimplePlaceholder {

    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String replace(String text, ExtraData data) {
        if (text.contains("{overflow_count}")) {
            Object count = data.get(ExtraData.DATA_OVERFLOWCOUNT);
            text = replaceToken(text, "overflow_count", count == null ? "0" : String.valueOf(count));
        }
        return text;
    }
}
