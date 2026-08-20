package pl.kiosel.playerlist.bungeecord;

import lombok.Getter;
import org.bukkit.World;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
public class ServerWorldData implements Externalizable {
    private String name;

    private int playerCount;

    public ServerWorldData() {}

    public ServerWorldData(World w) {
        this.name = w.getName();
        this.playerCount = w.getPlayers().size();
    }

	public void readExternal(ObjectInput in) throws IOException {
        this.name = in.readUTF();
        this.playerCount = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.name);
        out.writeInt(this.playerCount);
    }
}