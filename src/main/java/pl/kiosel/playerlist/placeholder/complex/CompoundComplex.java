package pl.kiosel.playerlist.placeholder.complex;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.internal.ComplexText;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Grouping;

public class CompoundComplex implements ComplexPlaceholder, Tickable {
    
    private final String name;
    private final int refreshTicks;
    private final ArrayIterable<String> overflowFormat;
    private final ArrayIterable<String> ping;
    private final ArrayIterable<String> skin;
    private final ArrayIterable<String> removeEmpty;
    private final ComplexText header;
    private final List<Compound> compounds;
    
    public CompoundComplex(ConfigurationSection sec) {
        this.compounds = new ArrayList<>();
        this.name = sec.getName();
        this.refreshTicks = Math.max(1, sec.getInt("refreshTicks", 20));
        this.ping = this.get(sec, "ping", null);
        this.skin = this.get(sec, "skin", null);
        this.removeEmpty = this.get(sec, "hideEmpty", "true");
        this.header = new ComplexText(sec.get("header"), this.refreshTicks);
        this.overflowFormat = this.get(sec, "overflowFormat", null);

        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                int fixedSize = items.getInt(key + ".fixedSize", -1);
                Compound comp = new Compound();
                comp.fixedSize = fixedSize;
                comp.placeholder = key;
                this.compounds.add(comp);
            }
        }
    }
    
    private ArrayIterable<String> get(ConfigurationSection sec, String key, String def) {
        if (sec.isList(key)) {
            List<?> list = sec.getList(key);
            if (list != null) {
                String[] lines = new String[list.size()];
                int[] delay = new int[list.size()];
                for (int i = 0; i < list.size(); ++i) {
                    Object obj = list.get(i);
                    if (obj instanceof String) {
                        lines[i] = (String) obj;
                        delay[i] = this.refreshTicks;
                    } else if (obj instanceof Map) {
                        FreeMap map = new FreeMap((Map<?, ?>) obj, true);
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
        return new CompoundSession(viewer, grouping);
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
        this.ping.tick();
        this.skin.tick();
        this.removeEmpty.tick();
        this.header.tick();
    }
    
    static class Compound {
        String placeholder;
        int fixedSize;
    }
    
    class CompoundSession implements ComplexSession {
        private final List<ExtraData> datas;
        
        public CompoundSession(Object p, Grouping grouping) {
            this.datas = new ArrayList<>();
            for (Compound c : CompoundComplex.this.compounds) {
                ComplexPlaceholder complex = PlaceholderManager.getComplexPlaceholder(c.placeholder);
                if (complex == null) {
                    continue;
                }
                ComplexSession session = complex.newSession(p, grouping);
                int fixedSize = c.fixedSize;
                if (fixedSize < 0) {
                    fixedSize = session.getSize();
                }
                String overflow = complex.getOverflowFormat();
                int index = 0;
                while (true) {
                    int remaining = session.getSize() - index;
                    if (overflow == null) {
                        if (index >= fixedSize) {
                            break;
                        }
                    } else if (index + 1 >= fixedSize && remaining > 1) {
                        ExtraData over = new ExtraData().put(ExtraData.DATA_TEXT, PlaceholderManager.replace(overflow, new ExtraData().put(ExtraData.DATA_PLAYER, p).put(ExtraData.DATA_VIEWER, p).put(ExtraData.DATA_OVERFLOWCOUNT, remaining)));
                        this.datas.add(over);
                        break;
                    }
                    if (remaining <= 0) {
                        while (index < fixedSize) {
                            this.datas.add(new ExtraData().put(ExtraData.DATA_TEXT, ""));
                            ++index;
                        }
                        break;
                    }
                    ExtraData data = session.getValue(index);
                    this.datas.add(data);
                    ++index;
                }
            }
            boolean hideEmpty = Boolean.parseBoolean(PlaceholderManager.replaceViewer(
                    CompoundComplex.this.removeEmpty.getCurrent(), p));
            if ((!this.datas.isEmpty() || !hideEmpty) && CompoundComplex.this.header.length() > 0) {
                this.datas.add(0, CompoundComplex.this.header.getCurrentData(p, null, null, CompoundComplex.this.ping.getCurrent(), CompoundComplex.this.skin.getCurrent(), "VISIBLE"));
            }
        }
        
        @Override
        public int getSize() {
            return this.datas.size();
        }
        
        @Override
        public ExtraData getValue(int index) {
            return this.datas.get(index);
        }
    }
}
