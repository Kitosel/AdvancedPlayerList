package pl.kiosel.playerlist.internal;

import pl.kiosel.playerlist.util.DisplayData;
import pl.kiosel.playerlist.model.skin.SkinToolkit;

import java.util.Map;
import java.util.ArrayList;
import pl.kiosel.playerlist.tablist.TablistLayout;
import pl.kiosel.playerlist.tablist.TablistDisplay;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.rosacore.method.FreeList;
import pl.kiosel.rosacore.method.FreeMap;

public class LayoutParser {

    private static DisplayData[] get(ConfigurationSection sec, String key, String def, int refreshTicks) {
        if (sec == null) {
            return new DisplayData[] { new DisplayData(def, refreshTicks) };
        }
        if (sec.isList("frames")) {
            List<?> frames = sec.getList("frames");
			if (frames == null || frames.isEmpty()) {
				return new DisplayData[] { new DisplayData(def, refreshTicks) };
			}
            DisplayData[] result = new DisplayData[frames.size()];
            for (int index = 0; index < frames.size(); index++) {
                result[index] = new DisplayData(frames.get(index), refreshTicks);
            }
            return result;
        }
        if (sec.isList("lines")) {
            return new DisplayData[] { new DisplayData(sec.getList("lines"), refreshTicks) };
        }
        if (sec.isList(key)) {
            List<?> list = sec.getList(key);
			if (list == null || list.isEmpty()) {
				return new DisplayData[] { new DisplayData(def, refreshTicks) };
			}
            DisplayData[] lines = new DisplayData[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                lines[i] = new DisplayData(list.get(i), refreshTicks);
            }
            return lines;
        }
        return new DisplayData[] { new DisplayData(sec.get(key), refreshTicks) };
    }
    
    public static TablistDisplay parseDisplay(ConfigurationSection header, ConfigurationSection footer) {
        int headerRefresh = header == null ? 20 : header.getInt("refreshTicks", 20);
        int footerRefresh = footer == null ? 20 : footer.getInt("refreshTicks", 20);
        DisplayData[] h = get(header, "text", "", headerRefresh);
        DisplayData[] f = get(footer, "text", "", footerRefresh);
        return new TablistDisplay(h, f);
    }
    
    public static TablistLayout parseLayout(ConfigurationSection config) {
        int size = config.getInt("size");
        boolean[] hide = new boolean[4];
        List<LineData> lines = new ArrayList<>();
        int idx = 0;
        for (Map<?, ?> map : config.getMapList("columns")) {
            FreeMap free = new FreeMap(map);
            boolean hideEmpty = (boolean) free.getOrDefault("hideEmpty", false);
            FreeList list = free.getList("items");
            if (list == null) {
                continue;
            }

            if (idx < 4) {
                hide[idx] = hideEmpty;
            }
            ++idx;
            int index = 0;
            for (Object obj : list) {
                if (index >= 20) {
                    break;
                }
                if (obj instanceof String) {
                    LineData data = new LineData((String)obj);

                    lines.add(data);
                    ++index;
                }
                else {
                    if (!(obj instanceof Map)) {
                        continue;
                    }
                    FreeMap m = new FreeMap((Map<?, ?>)obj, true);
                    int amount = m.getI("amount");
                    if (amount < 0) {
                        amount = 20 - index;
                    }
                    else if (amount == 0) {
                        amount = 1;
                    }
                    String text = m.getString("text");
                    Integer ping = m.getInteger("ping");
                    String skin = m.getString("skin");
                    for (int i = 0; i < amount; ++i) {
                        if (index >= 20) {
                            break;
                        }
                        LineData data2 = new LineData(text);
                        data2.setPing(ping);
                        if (skin != null) {
                            data2.setSkin(SkinToolkit.getDefaultToolkit().getSkinPredicate(skin));
                        }
                        lines.add(data2);
                        ++index;
                    }
                }
            }
        }
        TablistLayout layout = new TablistLayout(lines, size, hide);
        layout.setDefaultPing(config.getString("defaultPing"));
        layout.setDefaultDisplay(config.getString("defaultDisplay"));
        layout.setDefaultSkin(config.getString("defaultSkin"));
        layout.setSize(size);
        return layout;
    }
}
