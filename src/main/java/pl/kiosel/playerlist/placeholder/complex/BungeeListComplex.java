package pl.kiosel.playerlist.placeholder.complex;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.internal.Tickable;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.internal.ArrayIterable;
import pl.kiosel.playerlist.model.Evaluator;
import pl.kiosel.playerlist.internal.SortOrder;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.rosacore.method.FreeMap;
import pl.kiosel.rosacore.method.Group;
import pl.kiosel.rosacore.method.Grouping;

@SuppressWarnings("unchecked")
public class BungeeListComplex<T> implements ComplexPlaceholder, Tickable {
    private final String name;
    private final ArrayIterable<FreeMap> text;
    private final ArrayIterable<String> order;
    private final ArrayIterable<String> sort;
    private final ArrayIterable<String> condition;
    private final ArrayIterable<String> skinUUID;
    private final ArrayIterable<String> overflowFormat;
    private final ArrayIterable<String> ping;
    private final int refreshTicks;
    private final ArrayIterable<String> groupName;
    private final ArrayIterable<String> priority;
    private final ArrayIterable<String> opacity;
    private final Supplier<List<T>> supplier;
    private final String dataKey;
    private final Class<T> decl;

    class PlayerListSession implements ComplexSession {
        private final Object viewer;

        private List<T> objects;

        public PlayerListSession(Object v) {
            this.viewer = v;
        }

        public int getSize() {
            return this.objects.size();
        }

        public ExtraData getValue(int index) {
            T object = this.objects.get(index);
            FreeMap data = BungeeListComplex.this.text.getCurrent();
            return (new ExtraData()).put(ExtraData.DATA_TEXT, replace(object, data.getString("text")))
                    .put(ExtraData.DATA_PING,
                            replace(object, data.containsKey("ping") ? data.getString("ping") : BungeeListComplex.this.ping.getCurrent()))
                    .put(ExtraData.DATA_SKIN,
                            replace(object, data.containsKey("skin") ? data.getString("skin") : BungeeListComplex.this.skinUUID.getCurrent()))
                    .put(ExtraData.DATA_OPACITY,
                            replace(object, data.containsKey("opacity") ? data.getString("opacity") : BungeeListComplex.this.opacity.getCurrent()));
        }

        public void handle() {
            if (BungeeListComplex.this.sort != null)
                this.objects = SortOrder.order(BungeeListComplex.this.order.getCurrent()).sort(this.viewer, BungeeListComplex.this.sort.getCurrent(), BungeeListComplex.this.dataKey, this.objects);
        }

        private String replace(T object, String text) {
            return PlaceholderManager.replace(text, (
                    new ExtraData()).put(BungeeListComplex.this.dataKey, object).put(ExtraData.DATA_VIEWER, this.viewer));
        }
    }

    public BungeeListComplex(String name, Map<?, ?> text, String order, String sort, String condition,
                             String skin, String overflowFormat, String ping, String groupName,
                             String priority, int refreshTicks, Class<T> cl, String key,
                             String opacity, Supplier<List<T>> sup) {
        this.decl = cl;
        this.dataKey = key;
        this.supplier = sup;
        this.refreshTicks = Math.max(1, refreshTicks);
        this.name = name;
        this.text = new ArrayIterable(new FreeMap[] { new FreeMap(text) }, new int[] { refreshTicks });
        this.order = wrap(order);
        this.sort = wrap(sort);
        this.condition = wrap(condition);
        this.skinUUID = wrap(skin);
        this.overflowFormat = wrap(overflowFormat);
        this.ping = wrap(ping);
        this.groupName = wrap(groupName);
        this.priority = wrap(priority);
        this.opacity = wrap(opacity);
    }

    public String getOverflowFormat() { return this.overflowFormat.getCurrent(); }
    public String name() { return this.name; }

    public ComplexSession newSession(Object viewer, Grouping grouping) {
        return newUnknownSession(viewer, grouping);
    }

    public ComplexSession newUnknownSession(Object unknown, Grouping grouping) {
        List<T> objects;
        PlayerListSession session = new PlayerListSession(unknown);
        Group group = null;
        String groupName = this.groupName.getCurrent();
        if (groupName != null)
            group = grouping.getGroup(this.decl, groupName, this.supplier);
        if (group == null) {
            objects = new java.util.ArrayList<>(this.supplier.get());
            objects.removeIf(object -> !Evaluator.meetCriteria(unknown, this.dataKey, object, this.condition.getCurrent()));
        } else {
            objects = group.fetch(object -> Evaluator.meetCriteria(unknown, this.dataKey, object, this.condition.getCurrent()));
        }
        session.objects = objects;
        session.handle();
        return session;
    }

    public void onRegistered() {
        Ticker.register(this);
    }

    public void onUnregistered() {
        Ticker.unregister(this);
    }

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
        this.opacity.tick();
    }

    private ArrayIterable<String> wrap(String s) {
        return new ArrayIterable<>(new String[] { s }, new int[] { this.refreshTicks });
    }
}
