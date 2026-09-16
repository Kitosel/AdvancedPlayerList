package pl.kiosel.playerlist;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import pl.kiosel.playerlist.command.AdminCommand;
import pl.kiosel.playerlist.command.FakePlayerCommand;
import pl.kiosel.playerlist.api.AdvancedPlayerListAPI;
import pl.kiosel.playerlist.bridge.BridgeClient;
import pl.kiosel.playerlist.config.ConfigFile;
import pl.kiosel.playerlist.internal.ComplexParser;
import pl.kiosel.playerlist.model.Diagnostics;
import pl.kiosel.playerlist.model.Evaluator;
import pl.kiosel.playerlist.internal.InternalLayoutHandler;
import pl.kiosel.playerlist.internal.LayoutParser;
import pl.kiosel.playerlist.model.OfflinePlayerDatabase;
import pl.kiosel.playerlist.model.PlayerBank;
import pl.kiosel.playerlist.listener.PlayerListener;
import pl.kiosel.playerlist.listener.TabListener;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.internal.UUIDSet;
import pl.kiosel.playerlist.placeholder.Placeholder;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.complex.BukkitListComplex;
import pl.kiosel.playerlist.placeholder.complex.BungeePlayerListComplex;
import pl.kiosel.playerlist.placeholder.complex.CompoundComplex;
import pl.kiosel.playerlist.placeholder.complex.RemoteComplex;
import pl.kiosel.playerlist.placeholder.complex.TextComplex;
import pl.kiosel.playerlist.placeholder.parameterized.CustomParameterized;
import pl.kiosel.playerlist.placeholder.parameterized.InternalParameterized;
import pl.kiosel.playerlist.placeholder.parameterized.PlayerParameterized;
import pl.kiosel.playerlist.placeholder.simple.*;
import pl.kiosel.playerlist.protocol.Protocol;
import pl.kiosel.playerlist.protocol.ProtocolListener;
import pl.kiosel.playerlist.tablist.TablistProfileManager;
import pl.kiosel.playerlist.model.skin.SkinToolkit;
import pl.kiosel.playerlist.tablist.TablistDisplay;
import pl.kiosel.playerlist.tablist.TablistLayout;
import pl.kiosel.playerlist.tablist.TablistManager;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.playerlist.util.Utils;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.utils.Metrics;
import pl.kiosel.rosacore.utils.ReflectionUtils;
import pl.kiosel.rosacore.version.Version;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public final class AdvancedPlayerList extends RosaPlugin {

    @Getter private static AdvancedPlayerList instance;

    @Getter private RosaConfig configFile;
    @Getter private RosaConfig handlerFile;
    @Getter private RosaConfig globalFile;

    private final Map<String, RosaConfig> worldConfigFiles = new LinkedHashMap<>();
    @Getter private final Map<World, TablistLayout> layouts = new HashMap<>();
    @Getter private final Map<World, TablistDisplay> displays = new HashMap<>();
    @Getter private TablistManager tablistManager;
    @Getter private InternalLayoutHandler layoutHandler;
    @Getter private AdvancedPlayerListAPI api;
	@Getter private TablistProfileManager profileManager;
    @Getter private BridgeClient bridgeClient;

    public TablistDisplay globalDisplay;
    public TablistLayout globalLayout;

    @Getter private static boolean placeholderAPI = true;
    @Getter private static boolean economy = false;

    @Getter private PlayerBank playerBank;
    @Getter private OfflinePlayerDatabase offlinePlayerDatabase;
    @Getter private ProtocolListener protocolListener;
    private boolean fullyStarted;

    @Override
    public void onPluginLoad() {
        instance = this;
        setDev(false);
        useNMS(true);
    }

    @Override
    public void onPluginEnable() {
        new Metrics(this, 33982);

		Diagnostics.clear();
		placeholderAPI = true;
		fullyStarted = false;
        worldConfigFiles.clear();
        configFile = loadConfig(ConfigFile.CONFIG.getPath());
        handlerFile = loadConfig(ConfigFile.HANDLER.getPath());
        globalFile = loadConfig(ConfigFile.GLOBAL.getPath());
		profileManager = new TablistProfileManager(this, configFile, handlerFile, globalFile);
		profileManager.reloadSelection();

        setLocale(configFile.getString("locale"));

        if (!Evaluator.initialize()) {
            Utils.sendEngineMessage();
            emergencyStop();
            return;
        }
        checkDependencies();

        log("&aCompatibility mode: &fMinecraft "
                + Version.getServerVersion()
                + " &7(" + (Protocol.usesModernPlayerInfo() ? "modern" : "legacy") + ")"
                + "&f, Java " + ReflectionUtils.JAVA_VERSION
                + "&f, scripts: " + Evaluator.getEngineSource());

        if (getHookManager().getEconomy().isAvailable(configFile.getString("economy"))) {
            getHookManager().getEconomy().setPreferredHook(configFile.getString("economy"));
            economy = true;
        }

        playerBank = new PlayerBank(this);
        tablistManager = new TablistManager(this);
        layoutHandler = new InternalLayoutHandler(null);
        offlinePlayerDatabase = new OfflinePlayerDatabase();
        bridgeClient = new BridgeClient(this);

        Ticker.initialize(this);

        this.registerCommands("advancedplayerlist",
                new AdminCommand(this),
                new FakePlayerCommand(this)
        );

        this.registerListeners(
                new PlayerListener(this),
                new TabListener(this)
        );

        PlaceholderManager.register(new FakeSimple(),
                new InternalParameterized(),
                new PlayerParameterized(),
                new OverflowSimple(),
                new ColorizerSimple());
        if (placeholderAPI) {
            PlaceholderManager.register(new PlaceholderAPISimple());
        }
        if (economy)
            PlaceholderManager.register(new EconomySimple(this));

        protocolListener = new ProtocolListener(this);
        UUIDSet.set();

        loadSkinCache();
        applyConfiguration();
        playerBank.loadPlayerBank();

        api = new AdvancedPlayerListAPI(this);
        registerListeners(api);
    }

    @Override
    public void onPluginDisable() {
        try {
			if (tablistManager != null) {
				tablistManager.setTablistEnabled(false);
			}
            if (protocolListener != null) {
                protocolListener.disable();
                protocolListener = null;
            }
            if (api != null) {
                api.close();
                api = null;
            }
            if (bridgeClient != null) {
                bridgeClient.close();
				bridgeClient = null;
            }
			profileManager = null;
            HandlerList.unregisterAll((Plugin) this);
        } finally {
            Ticker.shutdown();
        }

        if (fullyStarted && playerBank != null) {
            saveSkinCache();
            playerBank.savePlayerBank();
        }
        fullyStarted = false;
		tablistManager = null;
		layoutHandler = null;
		playerBank = null;
		offlinePlayerDatabase = null;
        instance = null;
    }

    @Override
    public void onDataLoad() {
        fullyStarted = true;
    }

    @Override
    public void onConfigReload() {
        setLocale(configFile.getString("locale"));
		profileManager.reloadSelection();
        applyConfiguration();
    }

	public void applyActiveProfileConfiguration() {
		applyConfiguration();
	}

    private void applyConfiguration() {
        this.tablistManager.setTablistEnabled(false);
        PlaceholderManager.clearMissingPlaceholders();
        Ticker.setPeriodTicks(this.configFile.getInt("task-interval", 20));
        bridgeClient.configure(
                this.configFile.getBoolean("bridge.enabled", false),
                this.configFile.getInt("bridge.update-interval", 40));
        SkinToolkit.getDefaultToolkit().setMineSkinApiKey(this.configFile.getString("skin.mineskin-api-key", ""));
        loadOfflinePlayerSettings();
        loadFakePlayerDefaults(this.configFile.getConfigurationSection("fake-player.default-placeholder"));

        Evaluator.clearBindings();
        Evaluator.putBindings("server", getServer());
        loadScriptBindings(this.configFile.getConfigurationSection("script-engine.bindings"));

        PlaceholderManager.unregisterIf(this::isConfigurationPlaceholder);
        ComplexParser.parseAndRegister(rootSection(profileManager.getActiveHandler()));
        registerCustomPlaceholders(this.configFile.getConfigurationSection("custom-placeholders"));

        loadGlobalLayout(rootSection(profileManager.getActiveGlobal()));
        loadWorldLayouts();

        tablistManager.setTablistEnabled(this.configFile.getBoolean("tablist-enabled"));
    }

    private void checkDependencies() {
        PluginManager manager = getServer().getPluginManager();
        if (!manager.isPluginEnabled("PlaceholderAPI")) {
            Utils.sendPlaceholderMessage();
            placeholderAPI = false;
        }
    }

    private boolean isConfigurationPlaceholder(Placeholder placeholder) {
        Class<?> type = placeholder.getClass();
        return type == BungeePlayerListComplex.class
                || type == CompoundComplex.class
                || type == BukkitListComplex.class
                || type == RemoteComplex.class
                || type == TextComplex.class
                || type == CustomParameterized.class;
    }

    private void loadScriptBindings(ConfigurationSection bindings) {
        if (bindings == null)
            return;

        for (String key : bindings.getKeys(false)) {
            Object value = bindings.get(key);
            Evaluator.putBindings(key, value);
            log("&fScript binding: &e" + key + "&f: &b" + value);
        }
    }

    private void loadOfflinePlayerSettings() {
        if (offlinePlayerDatabase == null)
            return;

        try {
            offlinePlayerDatabase.setMaxTimePurging(getConfigFile().getString("offline-players.purgetime"));
        } catch (RuntimeException exception) {
            Diagnostics.record("Offline player purge time", exception);
            getRosaLogger().log(Level.WARNING, "Invalid offline player purge time; using the previous value", exception);
        }
        offlinePlayerDatabase.initialize();
    }

    private void loadFakePlayerDefaults(ConfigurationSection defaults) {
        FakePlayer.GLOBAL_PLACEHOLDER.clear();
        if (defaults != null) {
            for (String key : defaults.getKeys(false)) {
                FakePlayer.GLOBAL_PLACEHOLDER.put(key, Objects.toString(defaults.get(key), ""));
            }
        }

        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_name", "{fakeplayer_name}");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_displayname", "{fakeplayer_name}");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_listname", "{fakeplayer_name}");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_uuid", "{fakeplayer_uuid}");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_ping", "0");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_is_op", "false");
        FakePlayer.GLOBAL_PLACEHOLDER.putIfAbsent("player_world", "");
    }

    private void registerCustomPlaceholders(ConfigurationSection placeholders) {
        Map<String, String> scripts = new HashMap<>();
        if (placeholders != null) {
            for (String key : placeholders.getKeys(false)) {
                ConfigurationSection section = placeholders.getConfigurationSection(key);
                if (section == null) {
					putCustomScript(scripts, key, placeholders.getString(key));
                    continue;
                }

                String condition = section.getString("condition");
                String positive = section.getString("true");
                String negative = section.getString("false");
                if (condition == null || positive == null || negative == null) {
                    getRosaLogger().warning("Skipped incomplete custom placeholder: " + key);
                    continue;
                }

                String expression = condition + " ? '" + escapeScriptString(positive)
                        + "' : '" + escapeScriptString(negative) + "'";
                if (section.getBoolean("requireParams", false)) {
                    expression = "param == null ? '' : (" + expression + ")";
                }
				putCustomScript(scripts, key, expression);
            }
        }
        PlaceholderManager.register(new CustomParameterized(scripts));
    }

	private void putCustomScript(Map<String, String> scripts, String identifier, String script) {
		if (script == null) {
			getRosaLogger().warning("Skipped custom placeholder without a script: " + identifier);
			return;
		}
		if (PlaceholderManager.hasParameterizedPlaceholder(identifier)) {
			getRosaLogger().warning("Skipped custom placeholder with conflicting identifier: " + identifier);
			return;
		}
		scripts.put(identifier, script);
	}

    private String escapeScriptString(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void loadGlobalLayout(ConfigurationSection configuration) {
        globalLayout = LayoutParser.parseLayout(configuration);
        globalDisplay = LayoutParser.parseDisplay(
                configuration.getConfigurationSection("header"),
                configuration.getConfigurationSection("footer"));
    }

    private void loadWorldLayouts() {
        layouts.clear();
        displays.clear();

        File worldsDirectory = new File(getDataFolder(), "worlds");
        if (!worldsDirectory.isDirectory() && !worldsDirectory.mkdirs()) {
            getRosaLogger().warning("Unable to create " + worldsDirectory.getPath());
            return;
        }
        File[] files = worldsDirectory.listFiles();
        if (files == null)
            return;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".yml")) {
                continue;
            }

            String worldName = file.getName().substring(0, file.getName().length() - 4);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                log("&cCannot register layout for world &e" + worldName
                        + "&c. Check the file name and make sure the world is loaded.");
                continue;
            }

            log("&fParsing world tablist for &e" + worldName);
            String relativePath = "worlds/" + file.getName();
            RosaConfig worldConfig = worldConfigFiles.get(relativePath);
            if (worldConfig == null) {
                worldConfig = loadConfig(relativePath);
                worldConfigFiles.put(relativePath, worldConfig);
            }
            ConfigurationSection configuration = rootSection(worldConfig);
            layouts.put(world, LayoutParser.parseLayout(configuration));
            displays.put(world, LayoutParser.parseDisplay(
                    configuration.getConfigurationSection("header"),
                    configuration.getConfigurationSection("footer")));
        }
    }

    private ConfigurationSection rootSection(RosaConfig config) {
        ConfigurationSection root = config.getConfigurationSection("");
        if (root == null) {
            throw new IllegalStateException(config.getPath() + " does not contain a valid YAML root");
        }
        return root;
    }

    private void loadSkinCache() {
        File file = new File(getDataFolder(), ConfigFile.SKIN_CACHE.getPath());
        if (!file.isFile())
            return;

        try (InputStream input = Files.newInputStream(file.toPath())) {
            SkinToolkit.getDefaultToolkit().loadCache(input);
        } catch (Throwable throwable) {
            Diagnostics.record("Skin cache load", throwable);
            getRosaLogger().log(Level.WARNING, "Unable to load " + ConfigFile.SKIN_CACHE.getPath(), throwable);
        }
    }

    private void saveSkinCache() {
        if (!getDataFolder().isDirectory())
            return;

        File file = new File(getDataFolder(), ConfigFile.SKIN_CACHE.getPath());
        try (OutputStream output = Files.newOutputStream(file.toPath())) {
            SkinToolkit.getDefaultToolkit().saveCache(output);
        } catch (Throwable throwable) {
            Diagnostics.record("Skin cache save", throwable);
            getRosaLogger().log(Level.WARNING, "Unable to save " + ConfigFile.SKIN_CACHE.getPath(), throwable);
        }
    }
}
