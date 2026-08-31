package pl.kiosel.playerlist.placeholder.complex;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.model.Evaluator;

import java.util.*;

import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Grouping;

public class TextComplex implements ComplexPlaceholder, Tickable {

    private final String name;
    private final List<ArrayIterable<FreeMap>> text;
    private final ArrayIterable<String> condition;
    private final ArrayIterable<String> skinUUID;
    private final ArrayIterable<String> overflowFormat;
    private final ArrayIterable<String> ping;
    private final ArrayIterable<String> opacity;
    private final List<ArrayIterable<FreeMap>> alternateText;
    private final int refreshTicks;
    
    private static ArrayIterable<FreeMap> getLine(Object object, int refreshTicks) {
        if (object instanceof List) {
            List<?> list = (List<?>)object;
            FreeMap[] lines = new FreeMap[list.size()];
            int[] delay = new int[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                Object obj = list.get(i);
                if (obj instanceof String) {
                    lines[i] = new FreeMap().putThen("text", obj);
                    delay[i] = refreshTicks;
                } else if (obj instanceof Map) {
                    FreeMap map = new FreeMap((Map<?, ?>)obj, true);
                    int del = map.getI("delay");
                    if (del <= 0) {
                        del = refreshTicks;
                    }
                    lines[i] = map;
                    delay[i] = del;
                }
            }
            return new ArrayIterable<>(lines, delay);
        }
        if (object instanceof String) {
            return new ArrayIterable<>(new FreeMap[] { new FreeMap().putThen("text", object) }, new int[] { refreshTicks });
        }
        return new ArrayIterable<>(new FreeMap[0], new int[0]);
    }
    
    private static List<ArrayIterable<FreeMap>> getLines(Object o, int refreshTicks) {
        if (o instanceof List) {
            List<?> list = (List<?>)o;
            ArrayList<ArrayIterable<FreeMap>> texts = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof Map) {
                    FreeMap map = new FreeMap(obj, true);
                    ArrayIterable<FreeMap> free = getLine(map.get("text"), refreshTicks);
                    for (int amount = map.getI("amount"), x = 0; x < amount; ++x) {
                        texts.add(free);
                    }
                } else {
                    texts.add(getLine(obj, refreshTicks));
                }
            }
            return texts;
        }
        if (o instanceof Map) {
            FreeMap map2 = new FreeMap(o, true);
            ArrayIterable<FreeMap> free2 = getLine(map2.get("text"), refreshTicks);
            List<ArrayIterable<FreeMap>> list2 = new ArrayList<>();
            for (int amount2 = map2.getI("amount"), j = 0; j < amount2; ++j) {
                list2.add(free2);
            }
            return list2;
        }
        if (o instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection)o;
            ArrayIterable<FreeMap> free2 = getLine(section.get("text"), refreshTicks);
            List<ArrayIterable<FreeMap>> list2 = new ArrayList<>();
            for (int amount2 = section.getInt("amount", 1), j = 0; j < amount2; ++j) {
                list2.add(free2);
            }
            return list2;
        }
        return new ArrayList<>(Collections.singletonList(getLine(o, refreshTicks)));
    }
    
    public TextComplex(ConfigurationSection section) {
        this.name = section.getName();
        this.refreshTicks = Math.max(1, section.getInt("refreshTicks", 20));
        this.text = getLines(section.get("text"), this.refreshTicks);
        this.alternateText = getLines(section.get("alternateText"), this.refreshTicks);
        this.condition = this.get(section, "condition", null);
        this.skinUUID = this.get(section, "skin", null);
        this.overflowFormat = this.get(section, "overflowFormat", null);
        this.ping = this.get(section, "ping", null);
        this.opacity = this.get(section, "opacity", "FULL");
    }
    
    private ArrayIterable<String> get(ConfigurationSection sec, String key, String def) {
        if (sec.isList(key)) {
            List<?> list = sec.getList(key);
            String[] lines = new String[list.size()];
            int[] delay = new int[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                final Object obj = list.get(i);
                if (obj instanceof String) {
                    lines[i] = (String)obj;
                    delay[i] = this.refreshTicks;
                } else if (obj instanceof Map) {
                    FreeMap map = new FreeMap((Map<?, ?>)obj, true);
                    String t = map.getString("text");
                    int del = map.getI("delay");
                    if (t == null)
                        t = def;
                    if (del <= 0)
                        del = this.refreshTicks;
                    lines[i] = t;
                    delay[i] = del;
                }
            }
            return new ArrayIterable<>(lines, delay);
        }
        return new ArrayIterable<>(new String[] { sec.getString(key, def) }, new int[] { this.refreshTicks });
    }
    
    @Override
    public String getOverflowFormat() {
        return this.overflowFormat.getCurrent();
    }
    
    @Override
    public String name() {
        return this.name;
    }
    
    @Override
    public ComplexSession newSession(Object viewer, Grouping grouping) {
        return new TextSession(viewer);
    }
    
    @Override
    public void onRegistered() {
        Ticker.register(this);
    }
    
    @Override
    public void onUnregistered() {
        Ticker.unregister(this);
    }
    
    @Override
    public void run() {
        for (final ArrayIterable<FreeMap> element : this.text) {
            element.tick();
        }
        for (final ArrayIterable<FreeMap> element : this.alternateText) {
            element.tick();
        }
        this.condition.tick();
        this.overflowFormat.tick();
        this.ping.tick();
        this.skinUUID.tick();
        this.opacity.tick();
    }
    
    class TextSession implements ComplexSession {
        private final Object viewer;
        private final List<ArrayIterable<FreeMap>> current;
        
        public TextSession(Object v) {
            viewer = v;
            if (Evaluator.meetCriteria(viewer, ExtraData.DATA_PLAYER, viewer, TextComplex.this.condition.getCurrent())) {
                current = TextComplex.this.text;
            } else {
                current = TextComplex.this.alternateText;
            }
        }
        
        @Override
        public int getSize() {
            return this.current.size();
        }
        
        @Override
        public ExtraData getValue(int index) {
            FreeMap data = this.current.get(index).getCurrent();
            return (data == null) ? null : new ExtraData()
                    .put(ExtraData.DATA_TEXT, replace(data.getString("text")))
                    .put(ExtraData.DATA_PING, replace(data.containsKey("ping") ? data.getString("ping") : TextComplex.this.ping.getCurrent()))
                    .put(ExtraData.DATA_SKIN, replace(data.containsKey("skin") ? data.getString("skin") : TextComplex.this.skinUUID.getCurrent()))
                    .put(ExtraData.DATA_OPACITY, replace(data.containsKey("opacity") ? data.getString("opacity") : TextComplex.this.opacity.getCurrent()));
        }
        
        private String replace(String text) {
            return PlaceholderManager.replace(text, new ExtraData().put(ExtraData.DATA_PLAYER, this.viewer).put(ExtraData.DATA_VIEWER, this.viewer));
        }
    }
}
