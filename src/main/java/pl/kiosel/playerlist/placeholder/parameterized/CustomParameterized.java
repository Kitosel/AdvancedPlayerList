package pl.kiosel.playerlist.placeholder.parameterized;

import pl.kiosel.playerlist.model.Evaluator;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import javax.script.SimpleBindings;

import pl.kiosel.playerlist.placeholder.ExtraData;
import java.util.Map;
import pl.kiosel.playerlist.placeholder.ParameterizedPlaceholder;

public class CustomParameterized implements ParameterizedPlaceholder {

    private final Map<String, String> map;
    
    public CustomParameterized(Map<String, String> map) {
        this.map = map;
    }
    
    @Override
    public boolean accept(String placeholder) {
        return this.map.containsKey(placeholder);
    }
    
    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    @Override
    public String provide(String placeholder, String param, ExtraData data) {
        String map = this.map.get(placeholder);
        if (map != null) {
            SimpleBindings bindings = new SimpleBindings(Evaluator.getBindings());
            bindings.put("param", param);
            bindings.put("player", data.get(ExtraData.DATA_PLAYER));
            return String.valueOf(Evaluator.evaluate(PlaceholderManager.replace(map, data), bindings));
        }
        return null;
    }
}
