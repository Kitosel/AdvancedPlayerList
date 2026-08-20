package pl.kiosel.playerlist.model;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.util.RuntimeCompatibility;
import pl.kiosel.rosacore.RosaLogger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class Evaluator {

    private static final SimpleBindings simple = new SimpleBindings();
    private static final Set<String> errors = ConcurrentHashMap.newKeySet();

    private static ScriptEngine engine;
    @Getter
    private static String engineSource = "none";

    private Evaluator() {
    }

    public static synchronized boolean initialize() {
        if (engine != null) {
            return true;
        }

        if (RuntimeCompatibility.getJavaVersion() <= 11) {
            engine = findBundledEngine();
            if (engine != null) {
                engineSource = "built-in " + engine.getFactory().getEngineName();
            }
        }

        if (engine == null) {
            engine = findExternalEngine();
            if (engine != null) {
                engineSource = "JSEngine (" + engine.getFactory().getEngineName() + ")";
            }
        }

        if (engine == null) {
            engine = findBundledEngine();
            if (engine != null) {
                engineSource = "registered " + engine.getFactory().getEngineName();
            }
        }

        if (engine == null) {
            return false;
        }

        engine.setBindings(simple, ScriptContext.ENGINE_SCOPE);
        return true;
    }

    private static ScriptEngine findBundledEngine() {
        ScriptEngineManager manager = new ScriptEngineManager(Evaluator.class.getClassLoader());
        String[] names = {"Nashorn", "nashorn", "JavaScript", "javascript", "js"};
        for (String name : names) {
            ScriptEngine candidate = manager.getEngineByName(name);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static ScriptEngine findExternalEngine() {
		if (Bukkit.getServer() == null) {
			return null;
		}
        Plugin plugin = Bukkit.getPluginManager().getPlugin("JSEngine");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }

        try {
            Class<?> engineClass = Class.forName(
                    "com.vk2gpz.jsengine.JSEngine",
                    true,
                    plugin.getClass().getClassLoader());
            Method getEngine = engineClass.getMethod("getEngine");
            Object result = getEngine.invoke(null);
            return result instanceof ScriptEngine ? (ScriptEngine) result : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            onceError(exception);
            return null;
        }
    }

	public static void clearBindings() {
        simple.clear();
    }

    public static synchronized Object evaluate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        if (!initialize()) {
            reportMissingEngine();
            return null;
        }
        try {
            return engine.eval(text);
        } catch (ScriptException exception) {
            if (exception.getMessage() != null && errors.add(exception.getMessage())) {
                RosaLogger.getInstance().warning("&cFAILED TO EVALUATE SCRIPT:" + text + " \n" + exception.getMessage());
            }
            return null;
        }
    }

    public static synchronized Object evaluate(String text, Bindings bindings) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        if (!initialize()) {
            reportMissingEngine();
            return null;
        }
        try {
            return engine.eval(text, bindings);
        } catch (ScriptException exception) {
            if (onceError(exception)) {
                RosaLogger.getInstance().warning(String.format("FAILED TO EVALUATE SCRIPT\n %s - %s", text, bindings));
            }
            return null;
        }
    }

    public static boolean evaluateCondition(String text) {
        Object result = evaluate(text);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return result instanceof Number && ((Number) result).intValue() >= 1;
    }

    public static Bindings getBindings() {
        return simple;
    }

    public static boolean meetCriteria(Object viewer, String key, Object object, String script) {
        String expression = PlaceholderManager.replace(
                script,
                new ExtraData().put(ExtraData.DATA_VIEWER, viewer).put(key, object),
                false);
        if (PlaceholderManager.containsUnresolvedPlaceholder(expression)) {
            String message = "Unresolved placeholder in condition: " + expression;
            if (errors.add(message) && RosaLogger.getInstance() != null) {
                RosaLogger.getInstance().warning(message);
            }
            return false;
        }
        return evaluateCondition(expression);
    }

    public static boolean onceError(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        if (errors.add(writer.toString())) {
            if (RosaLogger.getInstance() != null) {
                RosaLogger.getInstance().log(Level.WARNING, "Error: " + throwable);
            }
            return true;
        }
        return false;
    }

    public static void putBindings(String key, Object value) {
        simple.put(key, value);
    }

    private static void reportMissingEngine() {
        String message = "No JavaScript engine is available";
        if (errors.add(message) && RosaLogger.getInstance() != null) {
            RosaLogger.getInstance().warning(message);
        }
    }
}
