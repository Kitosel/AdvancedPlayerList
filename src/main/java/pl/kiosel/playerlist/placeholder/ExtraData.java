package pl.kiosel.playerlist.placeholder;

import java.io.Serializable;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import java.io.Externalizable;

@SuppressWarnings("unchecked")
public class ExtraData implements Externalizable {

    public static final String DATA_OVERFLOWCOUNT = "advancedplayerlist:overflowCount";
    public static final String DATA_PING = "advancedplayerlist:ping";
    public static final String DATA_SKIN = "advancedplayerlist:skin";
    public static final String DATA_TEXT = "advancedplayerlist:text";
    public static final String DATA_VIEWER = "advancedplayerlist:viewer";
    public static final String DATA_PLAYER = "advancedplayerlist:player";
    public static final String DATA_SERVER = "advancedplayerlist:server";
    public static final String DATA_WORLD = "advancedplayerlist:world";
    public static final String DATA_OPACITY = "advancedplayerlist:opacity";
    private final Map<String, Object> objs;
    
    public ExtraData() {
        this.objs = new HashMap<>();
    }
    
    public ExtraData(ExtraData other) {
        (this.objs = new HashMap<>()).putAll(other.objs);
    }
    
    public <T> T get(String key) {
        return (T)this.objs.get(key);
    }
    
    public <T> T get(String key, T def) {
        return (T)this.objs.getOrDefault(key, def);
    }
    
    public void remove(String key) {
        this.objs.remove(key);
    }
    
    public <T> void ifPresent(String key, Consumer<T> t) {
        Object obj = this.objs.get(key);
        if (obj != null) {
            t.accept((T) obj);
        }
    }
    
    public ExtraData put(String key, Object obj) {
        if (obj != null)
            this.objs.put(key, obj);
        return this;
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        Map<String, Object> map = (Map<String, Object>) in.readObject();
        objs.putAll(map);
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        HashMap<String, Object> obj = new HashMap<>(this.objs);
        objs.forEach((a, b) -> {
            if (!(b instanceof Serializable) && !(b instanceof Externalizable)) {
                obj.remove(a);
            }
        });
        out.writeObject(obj);
    }
}
