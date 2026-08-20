package pl.kiosel.playerlist.bungeecord;

import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.World;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

public class ServerData implements Externalizable {
    public static final Map<String, ServerData> SERVERS = new ConcurrentHashMap<>();

    @Getter private int playerCount;
    @Getter private int maxPlayers;
    @Getter protected boolean online;
    @Getter protected String serverName;

    private final List<ServerWorldData> worlds = new ArrayList<>();

    public ServerData fill(Server server) {
        this.worlds.clear();
        this.playerCount = server.getOnlinePlayers().size();
        this.maxPlayers = server.getMaxPlayers();
        for (World w : server.getWorlds())
            this.worlds.add(new ServerWorldData(w));
        return this;
    }

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.serverName = (String)in.readObject();
        this.playerCount = in.readInt();
        this.online = in.readBoolean();
        this.maxPlayers = in.readInt();
        int size = in.readInt();
        this.worlds.clear();
        for (int i = 0; i < size; i++) {
            ServerWorldData data = new ServerWorldData();
            data.readExternal(in);
            this.worlds.add(data);
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.serverName);
        out.writeInt(this.playerCount);
        out.writeBoolean(this.online);
        out.writeInt(this.maxPlayers);
        int size = this.worlds.size();
        out.writeInt(size);
        for (ServerWorldData wo : this.worlds)
            wo.writeExternal(out);
    }
}