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
    public String replace(String string, ExtraData data) {
        if (string.contains("{overflow_count}")) {
            Object count = data.get(ExtraData.DATA_OVERFLOWCOUNT);
            string = string.replace("{overflow_count}", count == null ? "0" : String.valueOf(count));
        }
        return string;
    }
}
