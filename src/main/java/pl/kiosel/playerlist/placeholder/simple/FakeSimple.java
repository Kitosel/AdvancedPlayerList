package pl.kiosel.playerlist.placeholder.simple;

import java.util.Map;

import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.SimplePlaceholder;

import static pl.kiosel.rosacore.utils.ColorUtils.color;

public class FakeSimple implements SimplePlaceholder {

    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String replace(String string, ExtraData data) {
        Object player = data.get(ExtraData.DATA_PLAYER);
        if (player instanceof FakePlayer) {
            FakePlayer dp = (FakePlayer) player;
            for (Map.Entry<String, String> entry : dp.placeholders().entrySet()) {
                string = string.replace("{" + entry.getKey() + "}", color(entry.getValue()));
            }
            for (Map.Entry<String, String> entry : FakePlayer.GLOBAL_PLACEHOLDER.entrySet()) {
                string = string.replace("{" + entry.getKey() + "}", color(entry.getValue()));
            }
            string = string.replace("{fakeplayer_name}", color(dp.getName())).replace("{fakeplayer_uuid}",
                    (dp.getUniqueId() == null) ? "" : dp.getUniqueId().toString());
        }
        return string;
    }
}
