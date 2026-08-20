package pl.kiosel.playerlist.api;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.ParameterizedPlaceholder;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AdvancedPlayerListAPI implements Listener, AutoCloseable {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9.:-]+");
    private static volatile AdvancedPlayerListAPI instance;

    private final AdvancedPlayerList plugin;
    private final Map<String, Registration> registrations = new LinkedHashMap<>();
    private boolean closed;

    public AdvancedPlayerListAPI(AdvancedPlayerList plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        synchronized (AdvancedPlayerListAPI.class) {
            if (instance != null && instance != this) {
                throw new IllegalStateException("AdvancedPlayerList API is already initialized");
            }
            instance = this;
        }
    }

    public static AdvancedPlayerListAPI get() {
        AdvancedPlayerListAPI current = instance;
        if (current == null || current.closed) {
            throw new IllegalStateException("AdvancedPlayerList API is not available");
        }
        return current;
    }

    public synchronized void register(Plugin owner, AdvancedPlaceholder placeholder) {
        ensureOpen();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(placeholder, "placeholder");
		if (!owner.isEnabled()) {
			throw new IllegalStateException("Cannot register a placeholder for disabled plugin: "
					+ owner.getName());
		}

        String identifier = normalize(placeholder.getIdentifier());
        if (registrations.containsKey(identifier)) {
            throw new IllegalArgumentException("Placeholder is already registered: " + identifier);
        }
		if (PlaceholderManager.hasParameterizedPlaceholder(identifier)) {
			throw new IllegalArgumentException("Placeholder identifier conflicts with an existing placeholder: "
					+ identifier);
		}

        Registration registration = new Registration(owner, identifier, placeholder);
        registrations.put(identifier, registration);
        PlaceholderManager.register(registration);
        plugin.getDebug().debug("Registered external placeholder: " + identifier
                + " (owner: " + owner.getName() + ")");
    }

    public synchronized boolean unregister(Plugin owner, String identifier) {
        Objects.requireNonNull(owner, "owner");
        String normalized = normalize(identifier);
        Registration registration = registrations.get(normalized);
        if (registration == null || registration.owner != owner) {
            return false;
        }
        registrations.remove(normalized);
        PlaceholderManager.unregister(registration);
        return true;
    }

    public synchronized void unregisterAll(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        List<String> identifiers = new ArrayList<>();
        for (Registration registration : registrations.values()) {
            if (registration.owner == owner) identifiers.add(registration.identifier);
        }
        for (String identifier : identifiers) {
            Registration registration = registrations.remove(identifier);
            if (registration != null) PlaceholderManager.unregister(registration);
        }
    }

    public synchronized boolean isRegistered(String identifier) {
        return registrations.containsKey(normalize(identifier));
    }

    public synchronized List<String> getRegisteredIdentifiers() {
        return Collections.unmodifiableList(new ArrayList<>(registrations.keySet()));
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) unregisterAll(event.getPlugin());
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        for (Registration registration : new ArrayList<>(registrations.values())) {
            PlaceholderManager.unregister(registration);
        }
        registrations.clear();
        closed = true;
        synchronized (AdvancedPlayerListAPI.class) {
            if (instance == this) instance = null;
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("AdvancedPlayerList API is closed");
    }

    private static String normalize(String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        if (!IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid placeholder identifier: " + identifier
                    + ". Use letters, numbers, '.', ':' or '-'; '_' separates the parameter.");
        }
        return normalized;
    }

    private static final class Registration implements ParameterizedPlaceholder {

        private final Plugin owner;
        private final String identifier;
        private final AdvancedPlaceholder placeholder;

        private Registration(Plugin owner, String identifier, AdvancedPlaceholder placeholder) {
            this.owner = owner;
            this.identifier = identifier;
            this.placeholder = placeholder;
        }

        @Override
        public boolean accept(String name) {
            return identifier.equalsIgnoreCase(name);
        }

        @Override
        public String provide(String name, String parameter, ExtraData data) {
            if (!owner.isEnabled()) return null;
            return placeholder.onRequest(new PlaceholderContext(
                    data.get(ExtraData.DATA_PLAYER), data.get(ExtraData.DATA_VIEWER)), parameter);
        }

        @Override
        public void onRegistered() {
            placeholder.onRegistered();
        }

        @Override
        public void onUnregistered() {
            placeholder.onUnregistered();
        }
    }
}
