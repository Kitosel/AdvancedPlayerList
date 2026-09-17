package pl.kiosel.playerlist.tablist;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.api.TablistProfile;
import pl.kiosel.playerlist.api.TablistProfileInfo;
import pl.kiosel.rosacore.config.ConfigLoadResult;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.config.RosaConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TablistProfileManager {

	public static final String DEFAULT_PROFILE = "default";
	private static final String ACTIVE_PATH = "profiles.active";
	private static final String PREVIOUS_PATH = "profiles.previous";
	private static final Pattern PROFILE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

	private final AdvancedPlayerList plugin;
	private final RosaConfig settings;
	private final RosaConfig defaultHandler;
	private final RosaConfig defaultGlobal;

	private RosaConfig activeHandler;
	private RosaConfig activeGlobal;
	private String activeProfile = DEFAULT_PROFILE;

	public TablistProfileManager(AdvancedPlayerList plugin, RosaConfig settings,
								 RosaConfig defaultHandler, RosaConfig defaultGlobal) {
		this.plugin = plugin;
		this.settings = Objects.requireNonNull(settings, "settings");
		this.defaultHandler = Objects.requireNonNull(defaultHandler, "defaultHandler");
		this.defaultGlobal = Objects.requireNonNull(defaultGlobal, "defaultGlobal");
		this.activeHandler = defaultHandler;
		this.activeGlobal = defaultGlobal;
	}

	public synchronized void reloadSelection() {
		String requested = normalizeConfigured(settings.getString(ACTIVE_PATH, DEFAULT_PROFILE));
		try {
			Selection selection = loadSelection(requested);
			setSelection(selection);
		} catch (RuntimeException exception) {
			plugin.getRosaLogger().warning("Could not load tablist profile '" + requested
					+ "'; keeping profile '" + activeProfile + "': " + exception.getMessage());
		}
	}

	public synchronized void install(Plugin owner, TablistProfile profile) {
		ensurePrimaryThread();
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(profile, "profile");
		if (!owner.isEnabled()) {
			throw new IllegalStateException("Cannot install a profile for disabled plugin: " + owner.getName());
		}

		String id = normalizeId(profile.getId());
		if (DEFAULT_PROFILE.equals(id)) {
			throw new IllegalArgumentException("The default profile is reserved by AdvancedPlayerList");
		}
		validateYaml("global.yml", profile.getGlobalConfiguration());
		validateYaml("handler.yml", profile.getHandlerConfiguration());

		Path directory = profileDirectory(id);
		Metadata existing = readMetadata(directory.resolve("profile.yml"));
		if (existing != null && !existing.owner.equalsIgnoreCase(owner.getName())) {
			throw new IllegalStateException("Profile '" + id + "' belongs to " + existing.owner);
		}

		YamlConfiguration metadata = new YamlConfiguration();
		metadata.set("id", id);
		metadata.set("name", profile.getDisplayName());
		metadata.set("owner", owner.getName());
		try {
			Files.createDirectories(directory);
			writeAtomically(directory.resolve("global.yml"), profile.getGlobalConfiguration());
			writeAtomically(directory.resolve("handler.yml"), profile.getHandlerConfiguration());
			writeAtomically(directory.resolve("profile.yml"), metadata.saveToString());
		} catch (IOException exception) {
			throw new IllegalStateException("Could not install profile '" + id + "'", exception);
		}
	}

	public synchronized void activate(Plugin owner, String profileId) {
		Objects.requireNonNull(owner, "owner");
		String id = normalizeId(profileId);
		Metadata metadata = requireMetadata(id);
		if (!metadata.owner.equalsIgnoreCase(owner.getName())) {
			throw new IllegalStateException("Profile '" + id + "' belongs to " + metadata.owner);
		}
		activateInternal(id, true);
	}

	public synchronized void activate(String profileId) {
		activateInternal(normalizeId(profileId), true);
	}

	public synchronized String restore(Plugin owner) {
		Objects.requireNonNull(owner, "owner");
		if (DEFAULT_PROFILE.equals(activeProfile)) {
			throw new IllegalStateException("The calling plugin does not own the active profile");
		}
		Metadata metadata = requireMetadata(activeProfile);
		if (!metadata.owner.equalsIgnoreCase(owner.getName())) {
			throw new IllegalStateException("Active profile '" + activeProfile + "' belongs to " + metadata.owner);
		}
		return restore();
	}

	public synchronized String restore() {
		String previous = normalizeConfigured(settings.getString(PREVIOUS_PATH, DEFAULT_PROFILE));
		if (previous.equals(activeProfile)) {
			return activeProfile;
		}
		activateInternal(previous, true);
		return activeProfile;
	}

	public synchronized String getActiveProfile() {
		return activeProfile;
	}

	public synchronized String getPreviousProfile() {
		return normalizeConfigured(settings.getString(PREVIOUS_PATH, DEFAULT_PROFILE));
	}

	public synchronized RosaConfig getActiveHandler() {
		return activeHandler;
	}

	public synchronized RosaConfig getActiveGlobal() {
		return activeGlobal;
	}

	public synchronized List<TablistProfileInfo> getProfiles() {
		List<TablistProfileInfo> profiles = new ArrayList<>();
		profiles.add(new TablistProfileInfo(DEFAULT_PROFILE, "Default", plugin.getName(),
				DEFAULT_PROFILE.equals(activeProfile)));

		File[] directories = profilesDirectory().toFile().listFiles(File::isDirectory);
		if (directories != null) {
			for (File directory : directories) {
				String id;
				try {
					id = normalizeId(directory.getName());
				} catch (IllegalArgumentException ignored) {
					continue;
				}
				Metadata metadata = readMetadata(directory.toPath().resolve("profile.yml"));
				if (metadata != null && Files.isRegularFile(directory.toPath().resolve("global.yml"))
						&& Files.isRegularFile(directory.toPath().resolve("handler.yml"))) {
					profiles.add(new TablistProfileInfo(id, metadata.name, metadata.owner,
							id.equals(activeProfile)));
				}
			}
		}
		profiles.sort(Comparator.comparing(TablistProfileInfo::getId));
		return Collections.unmodifiableList(profiles);
	}

	private void activateInternal(String id, boolean updateHistory) {
		ensurePrimaryThread();
		Selection candidate = loadSelection(id);
		Selection previousSelection = new Selection(activeProfile, activeHandler, activeGlobal);
		String previousHistory = getPreviousProfile();

		if (updateHistory && !id.equals(activeProfile)) {
			settings.set(PREVIOUS_PATH, activeProfile);
		}
		settings.set(ACTIVE_PATH, id);
		ConfigSaveResult saved = settings.save();
		if (!saved.isSuccess()) {
			settings.set(ACTIVE_PATH, previousSelection.id);
			settings.set(PREVIOUS_PATH, previousHistory);
			throw new IllegalStateException("Could not save the active profile: " + saved.getProblems());
		}

		setSelection(candidate);
		try {
			plugin.applyActiveProfileConfiguration();
		} catch (RuntimeException exception) {
			setSelection(previousSelection);
			settings.set(ACTIVE_PATH, previousSelection.id);
			settings.set(PREVIOUS_PATH, previousHistory);
			settings.save();
			try {
				plugin.applyActiveProfileConfiguration();
			} catch (RuntimeException rollbackFailure) {
				exception.addSuppressed(rollbackFailure);
			}
			throw new IllegalStateException("Could not activate profile '" + id + "'", exception);
		}
	}

	private Selection loadSelection(String id) {
		if (DEFAULT_PROFILE.equals(id)) {
			return new Selection(id, defaultHandler, defaultGlobal);
		}
		requireMetadata(id);
		String root = "profiles/" + id + "/";
		RosaConfig handler = load(root + "handler.yml");
		RosaConfig global = load(root + "global.yml");
		return new Selection(id, handler, global);
	}

	private RosaConfig load(String path) {
		RosaConfig config = new RosaConfig(plugin, path);
		ConfigLoadResult result = config.load();
		if (!result.isSuccess()) {
			throw new IllegalStateException("Invalid " + path + ": " + result.getProblems(), result.getCause());
		}
		return config;
	}

	private Metadata requireMetadata(String id) {
		if (DEFAULT_PROFILE.equals(id)) {
			return new Metadata("Default", plugin.getName());
		}
		Path directory = profileDirectory(id);
		Metadata metadata = readMetadata(directory.resolve("profile.yml"));
		if (metadata == null || !Files.isRegularFile(directory.resolve("global.yml"))
				|| !Files.isRegularFile(directory.resolve("handler.yml"))) {
			throw new IllegalArgumentException("Profile is not installed: " + id);
		}
		return metadata;
	}

	private Metadata readMetadata(Path path) {
		if (!Files.isRegularFile(path)) {
			return null;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
		String name = yaml.getString("name");
		String owner = yaml.getString("owner");
		return name == null || name.trim().isEmpty() || owner == null || owner.trim().isEmpty()
				? null : new Metadata(name, owner);
	}

	private void setSelection(Selection selection) {
		this.activeProfile = selection.id;
		this.activeHandler = selection.handler;
		this.activeGlobal = selection.global;
	}

	private Path profilesDirectory() {
		return plugin.getDataFolder().toPath().resolve("profiles");
	}

	private Path profileDirectory(String id) {
		Path root = profilesDirectory().toAbsolutePath().normalize();
		Path requested = root.resolve(id).normalize();
		if (!requested.startsWith(root)) {
			throw new IllegalArgumentException("Profile must stay inside the profiles directory");
		}
		return requested;
	}

	private static String normalizeConfigured(String id) {
		try {
			return normalizeId(id);
		} catch (IllegalArgumentException ignored) {
			return DEFAULT_PROFILE;
		}
	}

	private static String normalizeId(String id) {
		Objects.requireNonNull(id, "profileId");
		String normalized = id.trim().toLowerCase(Locale.ROOT);
		if (!PROFILE_ID.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Invalid profile id: " + id
					+ ". Use lowercase letters, numbers, '.', '_' or '-'.");
		}
		return normalized;
	}

	private static void validateYaml(String name, String contents) {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(contents);
		} catch (InvalidConfigurationException exception) {
			throw new IllegalArgumentException("Invalid " + name, exception);
		}
		if (yaml.getKeys(false).isEmpty()) {
			throw new IllegalArgumentException(name + " cannot be empty");
		}
	}

	private static void writeAtomically(Path target, String contents) throws IOException {
		Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString() + ".", ".tmp");
		try {
			Files.write(temporary, contents.getBytes(StandardCharsets.UTF_8));
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void ensurePrimaryThread() {
		if (!Bukkit.isPrimaryThread()) {
			throw new IllegalStateException("Tablist profiles must be changed on the server thread");
		}
	}

	private static final class Selection {
		private final String id;
		private final RosaConfig handler;
		private final RosaConfig global;

		private Selection(String id, RosaConfig handler, RosaConfig global) {
			this.id = id;
			this.handler = handler;
			this.global = global;
		}
	}

	private static final class Metadata {
		private final String name;
		private final String owner;

		private Metadata(String name, String owner) {
			this.name = name;
			this.owner = owner;
		}
	}
}
