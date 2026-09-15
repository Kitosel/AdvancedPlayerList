package pl.kiosel.playerlist.model;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.rosacore.RosaLogger;
import pl.kiosel.rosacore.utils.ReflectionUtils;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class Evaluator {

    private static final String[] ENGINE_NAMES = {"Nashorn", "nashorn", "JavaScript", "javascript", "js"};
    private static final SimpleBindings simple = new SimpleBindings();

    @Getter
    private static final Set<String> errors = ConcurrentHashMap.newKeySet();
    @Getter
    private static String engineSource = "none";

    private static ScriptEngine engine;
    private static boolean initialized;

    private Evaluator() {
    }

    public static synchronized boolean initialize() {
        if (initialized) {
            return true;
        }
        initialized = true;

        if (ReflectionUtils.JAVA_VERSION < 15) {
            engine = createFactoryEngine("jdk.nashorn.api.scripting.NashornScriptEngineFactory");
            if (engine != null) {
                engineSource = "built-in " + engineName(engine);
            }
        }

        if (engine == null) {
            engine = findExternalEngine();
            if (engine != null) {
                engineSource = "JSEngine (" + engineName(engine) + ")";
            }
        }

        if (engine == null) {
            engine = createFactoryEngine("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
            if (engine != null) {
                engineSource = "downloaded " + engineName(engine);
            }
        }

        if (engine == null) {
            engine = findRegisteredEngine();
            if (engine != null) {
                engineSource = "registered " + engineName(engine);
            }
        }

        if (engine != null) {
            engine.setBindings(simple, ScriptContext.ENGINE_SCOPE);
        } else {
            engineSource = "native expressions";
        }
        return true;
    }

    private static ScriptEngine createFactoryEngine(String className) {
        Set<ClassLoader> loaders = Collections.newSetFromMap(new IdentityHashMap<>());
        loaders.add(Evaluator.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());

        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class<?> factoryClass = Class.forName(className, true, loader);
                Object factory = factoryClass.getDeclaredConstructor().newInstance();
                Object candidate = factoryClass.getMethod("getScriptEngine").invoke(factory);
                if (candidate instanceof ScriptEngine) {
                    return (ScriptEngine) candidate;
                }
            } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            }
        }
        return null;
    }

    private static ScriptEngine findRegisteredEngine() {
        Set<ClassLoader> loaders = Collections.newSetFromMap(new IdentityHashMap<>());
        loaders.add(Evaluator.class.getClassLoader());
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());

        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                ScriptEngineManager manager = new ScriptEngineManager(loader);
                for (String name : ENGINE_NAMES) {
                    ScriptEngine candidate = manager.getEngineByName(name);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            } catch (LinkageError | ServiceConfigurationError | SecurityException ignored) {
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

    private static String engineName(ScriptEngine scriptEngine) {
        try {
            return scriptEngine.getFactory().getEngineName();
        } catch (RuntimeException ignored) {
            return scriptEngine.getClass().getSimpleName();
        }
    }

    public static void clearBindings() {
        simple.clear();
    }

    public static synchronized Object evaluate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        initialize();
        if (engine == null) {
            return evaluateNative(text, simple);
        }
        try {
            return engine.eval(text);
        } catch (ScriptException exception) {
            if (exception.getMessage() != null && errors.add(exception.getMessage())) {
                RosaLogger logger = RosaLogger.getInstance();
                if (logger != null) {
                    logger.warning("&cFAILED TO EVALUATE SCRIPT: " + text + "\n" + exception.getMessage());
                }
            }
            return null;
        }
    }

    public static synchronized Object evaluate(String text, Bindings bindings) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        initialize();
        Bindings effectiveBindings = bindings == null ? simple : bindings;
        if (engine == null) {
            return evaluateNative(text, effectiveBindings);
        }
        try {
            return engine.eval(text, effectiveBindings);
        } catch (ScriptException exception) {
            if (onceError(exception)) {
                RosaLogger logger = RosaLogger.getInstance();
                if (logger != null) {
                    logger.warning(String.format("FAILED TO EVALUATE SCRIPT\n %s - %s", text, effectiveBindings));
                }
            }
            return null;
        }
    }

    private static Object evaluateNative(String text, Bindings bindings) {
        try {
            return ExpressionEvaluator.evaluate(text, bindings);
        } catch (IllegalArgumentException exception) {
            String message = "Unsupported native expression: " + text + " (" + exception.getMessage() + ")";
            if (errors.add(message)) {
                RosaLogger logger = RosaLogger.getInstance();
                if (logger != null) {
                    logger.warning(message + ". Install/enable Nashorn or JSEngine for full JavaScript support.");
                }
            }
            return null;
        }
    }

    public static boolean evaluateCondition(String text) {
        Object result = evaluate(text);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return result instanceof Number && ((Number) result).doubleValue() != 0.0D;
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
}
