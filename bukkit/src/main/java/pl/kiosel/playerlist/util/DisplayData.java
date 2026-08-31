package pl.kiosel.playerlist.util;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.model.Evaluator;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.rosacore.method.FreeMap;

import java.util.Map;
import java.util.List;
import java.util.StringJoiner;

public class DisplayData {

    private int initialDelay;
    @Getter private String text;
    @Getter private String condition;
    @Getter @Setter private int delay;

    public DisplayData(String text) {
        this(text, 1);
    }
    
    public DisplayData(Object obj, int defaultDelay) {
		this.delay = Math.max(1, defaultDelay);
		this.initialDelay = this.delay;
		this.text = "";

        if (obj instanceof String) {
            this.text = (String)obj;
        } else if (obj instanceof List) {
            this.text = joinLines((List<?>) obj);
        } else if (obj instanceof Map) {
            FreeMap map = new FreeMap((Map<?, ?>)obj, true);
            Object configuredText = map.containsKey("lines") ? map.get("lines") : map.get("text");
            this.text = configuredText instanceof List
                    ? joinLines((List<?>) configuredText)
                    : configuredText == null ? "" : String.valueOf(configuredText);
            this.delay = map.getI("delay");
            if (this.delay <= 0) {
                this.delay = defaultDelay;
            }
            this.condition = map.getString("condition");
            this.initialDelay = this.delay;
		} else if (obj != null) {
			this.text = String.valueOf(obj);
        }
    }

    private static String joinLines(List<?> lines) {
        StringJoiner result = new StringJoiner("\n");
        for (Object line : lines) {
            result.add(line == null ? "" : String.valueOf(line));
        }
        return result.toString();
    }

	public void resetDelay() {
        this.delay = this.initialDelay;
    }

	public boolean skip(Player viewer) {
        return this.condition != null && !Evaluator.meetCriteria(viewer, ExtraData.DATA_PLAYER, viewer, this.condition);
    }
}
