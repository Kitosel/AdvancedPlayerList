package pl.kiosel.playerlist.placeholder.complex;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.internal.SortOrder;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.model.Evaluator;
import java.util.ArrayList;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import java.util.function.Supplier;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.internal.ComplexText;
import pl.kiosel.playerlist.placeholder.ComplexSortable;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Group;
import pl.kiosel.rosacore.method.Grouping;
import pl.kiosel.rosacore.utils.NumberUtils;

public class BukkitListComplex<T> implements ComplexPlaceholder, Tickable, ComplexSortable {

    private final String name;
    private final ComplexText text;
    private final ComplexText header;
    private final ArrayIterable<String> order;
    private final ArrayIterable<String> sort;
    private final ArrayIterable<String> condition;
    private final ArrayIterable<String> skinUUID;
    private final ArrayIterable<String> overflowFormat;
    private final ArrayIterable<String> ping;
    private final ArrayIterable<String> hideEmpty;
    private final int refreshTicks;
    private final ArrayIterable<String> groupName;
    private final ArrayIterable<String> priority;
    private final Supplier<List<T>> supplier;
    private final ArrayIterable<String> opacity;
    private final String dataKey;
    private final Class<T> decl;
    
    public BukkitListComplex(ConfigurationSection section, Class<T> cl, String key, Supplier<List<T>> sup) {
        this.decl = cl;
        this.dataKey = key;
        this.supplier = sup;
        this.refreshTicks = Math.max(1, section.getInt("refreshTicks", 20));
        this.name = section.getName();
        this.text = new ComplexText(section.get("text"), this.refreshTicks);
        this.header = new ComplexText(section.get("header"), this.refreshTicks);
        this.order = this.get(section, "order", "ASCEND");
        this.sort = this.get(section, "sorter", null);
        this.condition = this.get(section, "condition", null);
        this.skinUUID = this.get(section, "skin", null);
        this.overflowFormat = this.get(section, "overflowFormat", null);
        this.ping = this.get(section, "ping", null);
        this.groupName = this.get(section, "grouping.groupName", null);
        this.priority = this.get(section, "grouping.priority", "0");
        this.hideEmpty = this.get(section, "hideEmpty", "false");
        this.opacity = this.get(section, "opacity", "VISIBLE");
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
                        t = def;
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
        return NumberUtils.parseInt(this.priority.getCurrent());
    }
    
    @Override
    public String name() {
        return this.name;
    }
    
    @Override
    public ComplexSession newSession(Object viewer, Grouping grouping) {
        return this.newUnknownSession(viewer, grouping);
    }
    
    public ComplexSession newUnknownSession(Object unknown, Grouping grouping) {
        PlayerListSession session = new PlayerListSession(unknown);
        Group group = null;
        String groupName = this.groupName.getCurrent();
        if (groupName != null && grouping != null) {
            group = grouping.getGroup(this.decl, groupName, this.supplier);
        }
        List<Object> objects = new ArrayList<>();
        if (group == null) {
            objects.addAll(this.supplier.get());
            objects.removeIf(object -> !Evaluator.meetCriteria(unknown, this.dataKey, object, this.condition.getCurrent()));
        }
        else {
            objects.addAll(group.fetch(object -> Evaluator.meetCriteria(unknown, this.dataKey, object, this.condition.getCurrent())));
        }
        session.access(session, objects);
        session.handle();
        if ((!Boolean.parseBoolean(PlaceholderManager.replaceViewer(this.hideEmpty.getCurrent(), unknown)) || !objects.isEmpty()) && this.header.length() > 0) {
            ExtraData h = this.header.getCurrentData(unknown, null, null, this.ping.getCurrent(), this.skinUUID.getCurrent(), this.opacity.getCurrent());
            if (h != null) {
                objects.add(0, h);
            }
        }
        return session;
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
        this.hideEmpty.tick();
        this.priority.tick();
        this.skinUUID.tick();
        this.sort.tick();
        this.header.tick();
        this.opacity.tick();
    }
    
    public class PlayerListSession implements ComplexSession {
        private final Object viewer;
        private List<?> objects;
        
        public PlayerListSession(Object v) {
            this.viewer = v;
        }
        
        @Override
        public int getSize() {
            return this.objects.size();
        }
        
        @Override
        public ExtraData getValue(int index) {
            Object object = this.objects.get(index);
            if (object instanceof ExtraData) {
                return (ExtraData)object;
            }
            return BukkitListComplex.this.text.getCurrentData(this.viewer, BukkitListComplex.this.dataKey, object, BukkitListComplex.this.ping.getCurrent(), BukkitListComplex.this.skinUUID.getCurrent(), BukkitListComplex.this.opacity.getCurrent());
        }
        
        public void handle() {
            if (BukkitListComplex.this.sort != null) {
                this.objects = SortOrder.order(BukkitListComplex.this.order.getCurrent()).sort(this.viewer, BukkitListComplex.this.sort.getCurrent(), BukkitListComplex.this.dataKey, this.objects);
            }
        }
        
        public void access(PlayerListSession playerListSession, List<?> objects) {
            playerListSession.objects =  objects;
        }
    }
}