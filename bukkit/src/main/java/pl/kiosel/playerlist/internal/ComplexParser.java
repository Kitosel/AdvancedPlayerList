package pl.kiosel.playerlist.internal;

import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.bungeecord.ServerData;
import pl.kiosel.playerlist.bungeecord.ServerWorldData;
import pl.kiosel.playerlist.model.WorldGroup;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.World;
import java.util.ArrayList;

import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.complex.BungeePlayerListComplex;
import pl.kiosel.playerlist.placeholder.complex.CompoundComplex;
import pl.kiosel.playerlist.placeholder.complex.TextComplex;
import pl.kiosel.playerlist.placeholder.complex.RemoteComplex;
import pl.kiosel.playerlist.placeholder.complex.BukkitListComplex;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.playerlist.AdvancedPlayerList;

public class ComplexParser {

    private static void log(String o) {
        AdvancedPlayerList.getInstance().log(o);
    }
    
    public static void parseAndRegister(@NotNull ConfigurationSection section) {
        boolean bridgeEnabled = AdvancedPlayerList.getInstance().getBridgeClient() != null
                && AdvancedPlayerList.getInstance().getBridgeClient().isEnabled();
        for (String key : section.getKeys(false)) {
            String type = section.getString(key + ".type");
            if (type == null) {
                log("&cUnknown placeholder type for " + key);
            } else {
                ConfigurationSection sec = section.getConfigurationSection(key);
                if (sec == null) {
                    log("&cInvalid configuration section for " + key);
                    continue;
                }
                switch (type) {
                    case "PLAYER_LIST": {
                        PlaceholderManager.register(new BukkitListComplex<>(
                                sec, Object.class, ExtraData.DATA_PLAYER,
                                AdvancedPlayerList.getInstance().getPlayerBank()::getOnlinePlayers));
                        break;
                    }
                    case "SERVER_REMOTE_HANDLER": {
                        if (!bridgeEnabled) {
                            log("&cCannot register placeholder &e" + key + " &cbecause proxy bridge is disabled");
                            continue;
                        }
                        PlaceholderManager.register(new RemoteComplex(sec));
                        break;
                    }
                    case "TEXT": {
                        PlaceholderManager.register(new TextComplex(sec));
                        break;
                    }
                    case "COMPOUND": {
                        PlaceholderManager.register(new CompoundComplex(sec));
                        break;
                    }
                    case "BUNGEECORD_PLAYER_LIST":
                    case "NETWORK_PLAYER_LIST": {
                        if (!bridgeEnabled) {
                            log("&cCannot register placeholder &e" + key + " &cbecause proxy bridge is disabled");
                            continue;
                        }
                        PlaceholderManager.register(new BungeePlayerListComplex(sec));
                        break;
                    }
                    case "WORLD_LIST": {
                        ConfigurationSection groupSection = section.getConfigurationSection(key + ".groups");
                        boolean addNot = section.getBoolean(key + ".addNotListed", true);
                        boolean hideEmpty = section.getBoolean(key + ".hideEmpty");
                        List<World> notListed = new ArrayList<>(Bukkit.getWorlds());
                        List<WorldGroup> groups = new ArrayList<>();

                        if (groupSection != null) {
                            for (String k : groupSection.getKeys(false)) {
                                Object obj = section.get(key + ".groups." + k);
                                WorldGroup group = new WorldGroup(k);
                                List<World> li = group.getWorlds();

                                if (obj instanceof List) {
                                    for (Object o : (List<?>) obj) {
                                        World world = Bukkit.getWorld(String.valueOf(o));
                                        if (world != null) {
                                            notListed.remove(world);
                                            li.add(world);
                                        }
                                    }
                                } else {
                                    World wo2 = Bukkit.getWorld(String.valueOf(obj));
                                    if (wo2 != null) {
                                        notListed.remove(wo2);
                                        li.add(wo2);
                                    }
                                }
                                groups.add(group);
                            }
                        } else {
                            addNot = true;
                        }
                        if (addNot) {
                            for (World world : notListed) {
                                WorldGroup gr = new WorldGroup(world.getName());
                                gr.getWorlds().add(world);
                                groups.add(gr);
                            }
                        }
                        BukkitListComplex<WorldGroup> worldList =
                                new BukkitListComplex<>(sec, WorldGroup.class, ExtraData.DATA_WORLD, () -> {
                            List<WorldGroup> groupped = new ArrayList<>();
                            for (WorldGroup group : groups) {
                                List<Player> players = group.collectPlayers();

                                if (hideEmpty && players.isEmpty()) {
                                    continue;
                                } else {
                                    groupped.add(group);
                                }
                            }
                            return groupped;
                        });
                        PlaceholderManager.register(worldList);
                        break;
                    }
                    case "SERVER_LIST": {
                        if (!bridgeEnabled) {
                            log("&cCannot register placeholder &e" + key + " &cbecause proxy bridge is disabled");
                            continue;
                        }
                        PlaceholderManager.register(new BukkitListComplex<>(
                                sec, ServerData.class, ExtraData.DATA_SERVER, () -> new ArrayList<>(ServerData.SERVERS.values())));
                        break;
                    }
                    case "NETWORK_WORLD_LIST": {
                        if (!bridgeEnabled) {
                            log("&cCannot register placeholder &e" + key + " &cbecause proxy bridge is disabled");
                            continue;
                        }
                        PlaceholderManager.register(new BukkitListComplex<>(
                                sec, ServerWorldData.class, ExtraData.DATA_WORLD, () -> {
                                    List<ServerWorldData> worlds = new ArrayList<>();
                                    for (ServerData server : ServerData.SERVERS.values()) {
                                        worlds.addAll(server.getWorlds());
                                    }
                                    return worlds;
                                }));
                        break;
                    }
                    default:
                        break;
                }
                log("&fRegistered &e'" + key + "' &fas &b" + type + " &ftype ComplexPlaceholder");
            }
        }
    }
}
