package pl.kiosel.playerlist.internal;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ExtraData;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.rosacore.method.FreeMap;

import static pl.kiosel.playerlist.placeholder.ExtraData.*;

public class ComplexText {
    private final ArrayIterable<FreeMap> headers;
    
    public ComplexText(Object o, int refreshTicks) {
        if (o == null) {
            this.headers = new ArrayIterable<>(null, null);
            return;
        }
        if (o instanceof ConfigurationSection) {
            String text = ((ConfigurationSection)o).getString("text");
            String ping = ((ConfigurationSection)o).getString("ping");
            String skin = ((ConfigurationSection)o).getString("skin");
            this.headers = new ArrayIterable<>(new FreeMap[] {
                    new FreeMap().putThen("text", text).putThen("ping", ping).putThen("skin", skin) }, new int[] { refreshTicks });
        } else if (o instanceof Map) {
            this.headers = new ArrayIterable<>(new FreeMap[] { new FreeMap((Map<?, ?>)o) }, new int[] { refreshTicks });
        } else if (o instanceof List) {
            int size = ((List<?>)o).size();
            FreeMap[] maps = new FreeMap[size];
            int[] delays = new int[size];
            int index = 0;

            for (Object obj : (List<?>) o) {
                if (obj instanceof Map) {
                    maps[index] = new FreeMap((Map<?, ?>)obj, true);
                    if (maps[index].containsKey("delay")) {
                        delays[index] = maps[index].getI("delay");
                    } else {
                        delays[index] = refreshTicks;
                    }
                } else {
                    maps[index] = new FreeMap().putThen("text", String.valueOf(obj));
                    delays[index] = refreshTicks;
                }
                ++index;
            }
            this.headers = new ArrayIterable<>(maps, delays);
        } else {
            this.headers = new ArrayIterable<>(new FreeMap[] {
                    new FreeMap().putThen("text", String.valueOf(o)) }, new int[] { refreshTicks });
        }
    }
    
    public int length() {
        return this.headers.length();
    }

    public ExtraData getCurrentData(Object viewer, String dataKey, Object object, String defaultPing,
                                    String defaultSkin, String defaultOpacity) {
        ExtraData data = new ExtraData();
        FreeMap map = this.headers.getCurrent();

        if (map != null) {
            map.consume("text", text -> {
                if (text == null) {
                    data.put(DATA_TEXT, "");
                } else if (dataKey != null && object != null) {
                    data.put(DATA_TEXT, PlaceholderManager.replace(String.valueOf(text),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer).put(dataKey, object)));
                } else {
                    data.put(DATA_TEXT, PlaceholderManager.replace(String.valueOf(text),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer)));
                }
            });
            map.consume("opacity", opacity -> {
                if (opacity == null)
                    opacity = defaultOpacity;
                if (opacity == null)
                    return;
                if (dataKey != null && object != null) {
                    data.put(DATA_OPACITY, PlaceholderManager.replace(String.valueOf(opacity),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer).put(dataKey, object)));
                } else {
                    data.put(DATA_OPACITY, PlaceholderManager.replace(String.valueOf(opacity),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer)));
                }
            });
            map.consume("ping", ping -> {
                if (ping == null)
                    ping = defaultPing;
                if (ping == null)
                    return;
                if (dataKey != null && object != null) {
                    data.put(DATA_PING, PlaceholderManager.replace(String.valueOf(ping),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer).put(dataKey, object)));
                } else {
                    data.put(DATA_PING, PlaceholderManager.replace(String.valueOf(ping),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer)));
                }
            });

            map.consume("skin", skin -> {
                if (skin == null)
                    skin = defaultSkin;
                if (skin == null)
                    return;
                if (dataKey != null && object != null) {
                    data.put(DATA_SKIN, PlaceholderManager.replace(String.valueOf(skin),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer).put(dataKey, object)));
                } else {
                    data.put(DATA_SKIN, PlaceholderManager.replace(String.valueOf(skin),
                            (new ExtraData()).put(DATA_PLAYER, viewer).put(DATA_VIEWER, viewer)));
                }
            });
        } else {
            data.put(DATA_TEXT, "");
        }
        return data;
    }
    
    public FreeMap getCurrent() {
        return this.headers.getCurrent();
    }
    
    public void tick() {
        this.headers.tick();
    }
}
