package pl.kiosel.playerlist.placeholder.complex;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import java.util.ArrayList;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ComplexSession;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.internal.ComplexText;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.ComplexSortable;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.Bytes;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Grouping;

import static pl.kiosel.playerlist.placeholder.ExtraData.*;

public class BungeePlayerListComplex implements ComplexPlaceholder, Tickable, ComplexSortable {

    public ExtraData[] lines;
    private final String name;
    private final ComplexText text;
    private final ComplexText header;
    private final ArrayIterable<String> order;
    private final ArrayIterable<String> sort;
    private final ArrayIterable<String> condition;
    private final ArrayIterable<String> skinUUID;
    private final ArrayIterable<String> overflowFormat;
    private final ArrayIterable<String> ping;
    private final int refreshTicks;
    private final ArrayIterable<String> groupName;
    private final ArrayIterable<String> priority;
    private final ArrayIterable<String> hideEmpty;
    private final ArrayIterable<String> opacity;
    
    public BungeePlayerListComplex(ConfigurationSection section) {
        this.lines = new ExtraData[0];
        this.name = section.getName();
        this.refreshTicks = Math.max(1, section.getInt("refreshTicks", 20));
        this.text = new ComplexText(section.get("text"), this.refreshTicks);
        this.header = new ComplexText(section.get("header"), this.refreshTicks);
        this.order = this.get(section, "order", null);
        this.sort = this.get(section, "sort", null);
        this.condition = this.get(section, "condition", null);
        this.skinUUID = this.get(section, "skin", null);
        this.overflowFormat = this.get(section, "overflowFormat", null);
        this.ping = this.get(section, "ping", null);
        this.groupName = this.get(section, "grouping.groupName", null);
        this.priority = this.get(section, "grouping.priority", "0");
        this.hideEmpty = this.get(section, "hideEmpty", "false");
        this.opacity = this.get(section, "opacity", "FULL");
    }
    
    private ArrayIterable<String> get(ConfigurationSection sec, String key, String def) {
        if (sec.isList(key)) {
            List<?> list = sec.getList(key);
            String[] lines = new String[list.size()];
            int[] delay = new int[list.size()];

            for (int i = 0; i < list.size(); ++i) {
                Object obj = list.get(i);
                if (obj instanceof String) {
                    lines[i] = (String)obj;
                    delay[i] = this.refreshTicks;
                } else if (obj instanceof Map) {
                    FreeMap map = new FreeMap((Map<?, ?>)obj, true);
                    String t = map.getString("text");
                    int del = map.getI("delay");
                    if (t == null) {
                        t = "";
                    }
                    if (del <= 0) {
                        del = this.refreshTicks;
                    }
                    lines[i] = t;
                    delay[i] = del;
                }
            }
            return new ArrayIterable<>(lines, delay);
        }
        return new ArrayIterable<>(new String[] { sec.getString(key, def) }, new int[] { this.refreshTicks });
    }
    
    @Override
    public String getGroup() {
        return this.groupName.getCurrent();
    }
    
    @Override
    public String getOverflowFormat() {
        return this.overflowFormat.getCurrent();
    }
    
    @Override
    public int getPriority() {
        try {
            return Integer.parseInt(this.priority.getCurrent());
        } catch (Throwable t) {
            return 0;
        }
    }
    
    @Override
    public String name() {
        return this.name;
    }
    
    @Override
    public ComplexSession newSession(Object viewer, Grouping grouping) {
        return new RemoteSession(viewer);
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
        this.text.tick();
        this.condition.tick();
        this.groupName.tick();
        this.order.tick();
        this.overflowFormat.tick();
        this.ping.tick();
        this.priority.tick();
        this.skinUUID.tick();
        this.sort.tick();
        this.hideEmpty.tick();
        this.header.tick();
        this.opacity.tick();
        if (this.tickTime() % this.refreshTicks == 0) {
            final Bytes b = new Bytes();
            b.write("PlayerList");
            b.write(this.name);
            b.write(this.text.getCurrent().unwrap());
            b.write(this.order.getCurrent());
            b.write(this.sort.getCurrent());
            b.write(this.condition.getCurrent());
            b.write(this.skinUUID.getCurrent());
            b.write(this.overflowFormat.getCurrent());
            b.write(this.ping.getCurrent());
            b.write(this.groupName.getCurrent());
            b.write(this.priority.getCurrent());
            b.write(this.refreshTicks);
            b.write(this.opacity.getCurrent());
            b.close();
        }
    }
    
    class RemoteSession implements ComplexSession {
        private final Object viewer;
        private final ArrayList<ExtraData> datas;
        
        public RemoteSession(Object viewer) {
            this.datas = new ArrayList<>();
            this.viewer = viewer;
            ExtraData[] lx = BungeePlayerListComplex.this.lines.clone();

            if ((lx.length > 0
                    || !Boolean.parseBoolean(PlaceholderManager.replaceViewer(BungeePlayerListComplex.this.hideEmpty.getCurrent(), viewer)))
                    && BungeePlayerListComplex.this.header.length() > 0) {
                FreeMap h = BungeePlayerListComplex.this.header.getCurrent();
                ExtraData obj = new ExtraData();
                h.ifPresent("ping", ping -> obj.put(DATA_PING, ping));
                h.ifPresent("skin", skin -> obj.put(DATA_SKIN, skin));
                h.ifPresent("text", text -> obj.put(DATA_TEXT, text));
                h.ifPresent("opacity", opacity -> obj.put(DATA_OPACITY, opacity));
                this.datas.add(obj);
            }
            this.datas.addAll(Arrays.asList(lx));
        }
        
        @Override
        public int getSize() {
            return this.datas.size();
        }
        
        @Override
        public ExtraData getValue(int index) {
            ExtraData data = this.datas.get(index);

            if (data != null) {
                data.put(DATA_PING, this.replace(data.get(DATA_PING)));
                data.put(DATA_SKIN, this.replace(data.get(DATA_SKIN)));
                data.put(DATA_TEXT, this.replace(data.get(DATA_TEXT)));
                data.put(DATA_OPACITY, this.replace(data.get(DATA_OPACITY)));
            }
            return data;
        }
        
        private String replace(String text) {
            return PlaceholderManager.replace(text, new ExtraData().put(DATA_VIEWER, this.viewer).put(DATA_PLAYER, this.viewer));
        }
    }
}
