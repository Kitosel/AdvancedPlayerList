package pl.kiosel.playerlist.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.model.Evaluator;
import pl.kiosel.rosacore.RosaLogger;

import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public final class PlaceholderManager {
    private static final Set<SimplePlaceholder> SIMPLES = new LinkedHashSet<>();
    private static final List<ParameterizedPlaceholder> PARAMETERIZED = new CopyOnWriteArrayList<>();
    private static final List<ComplexPlaceholder> COMPLEX = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Integer> REPLACE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Pattern UNRESOLVED_NATIVE_PLACEHOLDER = Pattern.compile("\\{[A-Za-z0-9_.:-]+}");
    private static final Pattern UNRESOLVED_PAPI_PLACEHOLDER = Pattern.compile("%[^%\\s]+%");

    public static final Set<String> missingPlaceholders = ConcurrentHashMap.newKeySet();

    private PlaceholderManager() {
    }
    
    public static String extractString(String s, UnaryOperator<String> c) {
        Child child = new Child();
        char[] a = s.toCharArray();
        boolean escape = false;
        for (char n : a) {
            if (n == '\\' || escape) {
                if (escape) {
                    child.builder.append(n);
                }
                escape = !escape;
            } else if (n == '{') {
                Child.setChild(child, new Child());
                Child.setParent(child.child, child);
                child = child.child;
            } else if (n == '}') {
                if (child.parent == null) {
                    child.builder.append(n);
                } else {
                    String result = c.apply(child.builder.toString());
                    child.parent.builder.append((result == null) ? ("{" + child.builder + "}") : result);
                    child = child.parent;
                }
            } else {
                child.builder.append(n);
            }
        }
        return child.builder.toString();
    }
    
    public static ComplexPlaceholder getComplexPlaceholder(String name) {
        for (ComplexPlaceholder cp : COMPLEX) {
            if (cp.name().equals(name)) {
                return cp;
            }
        }
        return null;
    }
    
    public static List<ComplexPlaceholder> getComplexPlaceholders() {
        return COMPLEX;
    }
    
    public static List<ParameterizedPlaceholder> getParameterizedPlaceholder() {
        return PARAMETERIZED;
    }

	public static boolean hasParameterizedPlaceholder(String identifier) {
		if (identifier == null) return false;
		for (ParameterizedPlaceholder placeholder : PARAMETERIZED) {
			try {
				if (placeholder.accept(identifier)) return true;
			} catch (RuntimeException exception) {
				Evaluator.onceError(exception);
			}
		}
		return false;
	}

    public static void register(Placeholder placeholder) {
        boolean registered = false;
        if (placeholder instanceof SimplePlaceholder) {
            registered |= SIMPLES.add((SimplePlaceholder) placeholder);
        }
        if (placeholder instanceof ParameterizedPlaceholder) {
            registered |= PARAMETERIZED.add((ParameterizedPlaceholder) placeholder);
        }
        if (placeholder instanceof ComplexPlaceholder) {
            registered |= COMPLEX.add((ComplexPlaceholder) placeholder);
        }
        if (registered) {
            placeholder.onRegistered();
        }
    }

    public static void register(Placeholder... placeholders) {
        for (Placeholder placeholder : placeholders) {
            register(placeholder);
        }
    }
    
    public static String replaceViewer(String text, Object viewer) {
        return replace(text, new ExtraData().put(ExtraData.DATA_VIEWER, viewer).put(ExtraData.DATA_PLAYER, viewer));
    }
    
    public static String replace(String text, ExtraData data) {
        return replace(text, data, false);
    }
    
    public static String replace(String text, ExtraData data, boolean quoted) {
        if (text == null) {
            return null;
        }
        int depth = REPLACE_DEPTH.get();
        if (depth == 0) {
            missingPlaceholders.clear();
        }
        REPLACE_DEPTH.set(depth + 1);
        try {
            String result = replaceInternal(text, data == null ? new ExtraData() : data, quoted);
            if (depth == 0) {
                recordUnresolvedPlaceholders(result);
            }
            return result;
        } finally {
            if (depth == 0) {
                REPLACE_DEPTH.remove();
            } else {
                REPLACE_DEPTH.set(depth);
            }
        }
    }

    private static String replaceInternal(String text, ExtraData data, boolean quoted) {
        text = applySimplePlaceholders(text, data, false);
        for (ParameterizedPlaceholder param : getParameterizedPlaceholder()) {
            text = extractString(text, found -> {
                ExtraData dat = data;
                Object viewer;
                final String[] x;
                String provided;
                String string;

                if (found.startsWith("viewer_")) {
                    found = found.substring(7);
                    dat = new ExtraData(dat);
                    viewer = dat.get(ExtraData.DATA_VIEWER);
                    if (viewer != null) {
                        dat.put(ExtraData.DATA_PLAYER, viewer);
                    }
                }
                x = found.split("_", 2);
                try {
                    if (param.accept(x[0])) {
                        provided = param.provide(x[0], (x.length == 2) ? x[1] : null, dat);
                        if (provided == null) {
                            missingPlaceholders.add("{" + found + "}");
                            return null;
                        }
                        if (quoted) {
                            string = "'" + provided.replace("\\", "\\\\").replace("'", "\\'") + "'";
                        } else {
                            string = provided;
                        }
                        return string;
                    }
                } catch (Throwable t2) {
                    Evaluator.onceError(t2);
                }
                return null;
            });
        }
        return applySimplePlaceholders(text, data, true);
    }

    private static String applySimplePlaceholders(String text, ExtraData data, boolean repeatedPass) {
        for (SimplePlaceholder placeholder : SIMPLES) {
            if (repeatedPass && !placeholder.repeatAfterParameterizedPlaceholders()) {
                continue;
            }
            try {
                String replacement = placeholder.replace(text, data);
                if (replacement != null) {
                    text = replacement;
                }
            } catch (Throwable throwable) {
                RosaLogger logger = RosaLogger.getInstance();
                if (logger != null) {
                    logger.log(Level.WARNING, "Unable to resolve a placeholder", throwable);
                } else {
                    Evaluator.onceError(throwable);
                }
            }
        }
        return text;
    }

    public static Set<String> getRegisteredPlaceholderIdentifiers() {
        if (AdvancedPlayerList.isPlaceholderAPI())
            return PlaceholderAPI.getRegisteredIdentifiers();
        return null;
    }

    public static boolean containsUnresolvedPlaceholder(String text) {
        return text != null && (UNRESOLVED_NATIVE_PLACEHOLDER.matcher(text).find()
                || UNRESOLVED_PAPI_PLACEHOLDER.matcher(text).find());
    }

    private static void recordUnresolvedPlaceholders(String text) {
        if (text == null) {
            return;
        }
        recordMatches(text, UNRESOLVED_NATIVE_PLACEHOLDER);
        recordMatches(text, UNRESOLVED_PAPI_PLACEHOLDER);
    }

    private static void recordMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) missingPlaceholders.add(matcher.group());
    }
    
    public static void unregister(Placeholder placeholder) {
        boolean removed = SIMPLES.remove(placeholder)
                | PARAMETERIZED.remove(placeholder)
                | COMPLEX.remove(placeholder);
        if (removed) {
            placeholder.onUnregistered();
        }
    }

    public static void unregisterIf(Predicate<Placeholder> predicate) {
        for (ComplexPlaceholder placeholder : COMPLEX) {
            if (predicate.test(placeholder)) {
                unregister(placeholder);
            }
        }
        for (ParameterizedPlaceholder placeholder : PARAMETERIZED) {
            if (predicate.test(placeholder)) {
                unregister(placeholder);
            }
        }
        for (SimplePlaceholder placeholder : SIMPLES.toArray(new SimplePlaceholder[0])) {
            if (predicate.test(placeholder)) {
                unregister(placeholder);
            }
        }
    }
    
    static class Child {
        private final StringBuilder builder;
        private Child parent;
        private Child child;
        
        Child() {
            this.builder = new StringBuilder();
        }
        
        static void setChild(Child child, Child child2) {
            child.child = child2;
        }
        
        static void setParent(Child child, Child parent) {
            child.parent = parent;
        }
    }
}
