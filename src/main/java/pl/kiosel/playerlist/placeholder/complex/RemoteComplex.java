package pl.kiosel.playerlist.placeholder.complex;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import java.util.ArrayList;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import pl.kiosel.playerlist.internal.ComplexText;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.placeholder.ExtraData;
import java.util.Map;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.Bytes;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Grouping;

public class RemoteComplex implements ComplexPlaceholder, Tickable {

    private final Map<String, ExtraData[]> lines;
    private final String name;
    private final int refreshTicks;
    private final ArrayIterable<String> overflowFormat;
    private final ComplexText header;
    private final List<String> server;
    private final String handler;
    private final ArrayIterable<String> hideEmpty;
    
    public RemoteComplex(ConfigurationSection sec) {
        lines = new LinkedHashMap<>();
        name = sec.getName();
        handler = sec.getString("handler");
        refreshTicks = Math.max(1, sec.getInt("refreshTicks", 20));
        overflowFormat = this.get(sec, "overflowFormat", null);
        server = (sec.isList("server") ? sec.getStringList("server") : Arrays.asList(sec.getString("server")));
        header = new ComplexText(sec.get("header"), this.refreshTicks);
        hideEmpty = this.get(sec, "hideEmpty", "true");
        for (String s : server) {
            lines.put(s, new ExtraData[0]);
        }
    }
    
    public Map<String, ExtraData[]> getStorage() {
        return this.lines;
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
                    final FreeMap map = new FreeMap((Map<?, ?>)obj, true);
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
    public String getOverflowFormat() {
        return this.overflowFormat.getCurrent();
    }
    
    @Override
    public String name() {
        return this.name;
    }
    
    @Override
    public ComplexSession newSession(final Object viewer, final Grouping grouping) {
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
        this.overflowFormat.tick();
        this.header.tick();
        this.hideEmpty.tick();
        if (tickTime() % refreshTicks == 0) {
            for (String server : server) {
                Bytes b = new Bytes();
                b.write("RemotePlaceholder");
                b.write(server);
                b.write(this.name);
                b.write(this.handler);
                b.close();
            }
        }
    }
    
    class RemoteSession implements ComplexSession {

        private final Object viewer;
        private final ArrayList<ExtraData> lin;
        
        public RemoteSession(Object viewer) {
            this.lin = new ArrayList<>();
            this.viewer = viewer;
            for (Map.Entry<String, ExtraData[]> entry : RemoteComplex.this.lines.entrySet()) {
                for (ExtraData d : entry.getValue()) {
                    if (d != null) {
                        ExtraData clone = new ExtraData();
                        clone.put(ExtraData.DATA_PING, replace(d.get(ExtraData.DATA_PING)));
                        clone.put(ExtraData.DATA_SKIN, replace(d.get(ExtraData.DATA_SKIN)));
                        clone.put(ExtraData.DATA_TEXT, replace(d.get(ExtraData.DATA_TEXT)));
                        this.lin.add(clone);
                    }
                }
            }
            if ((!Boolean.parseBoolean(replace(RemoteComplex.this.hideEmpty.getCurrent())) || !lin.isEmpty()) && RemoteComplex.this.header.length() > 0) {
                lin.add(0, RemoteComplex.this.header.getCurrentData(viewer, null, null, null, null, "VISIBLE"));
            }
        }
        
        @Override
        public int getSize() {
            return this.lin.size();
        }
        
        @Override
        public ExtraData getValue(int index) {
            return this.lin.get(index);
        }
        
        private String replace(String text) {
            return PlaceholderManager.replaceViewer(text, viewer);
        }
    }
}
