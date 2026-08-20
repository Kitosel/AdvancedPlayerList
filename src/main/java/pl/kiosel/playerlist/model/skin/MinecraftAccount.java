package pl.kiosel.playerlist.model.skin;

import lombok.Getter;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.UUID;
import java.io.Externalizable;

public class MinecraftAccount implements Externalizable {

    @Getter protected boolean valid;
    @Getter protected String name;
    @Getter protected UUID uuid;

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        this.valid = true;
        this.name = in.readUTF();
        this.uuid = new UUID(in.readLong(), in.readLong());
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.name);
        out.writeLong(this.uuid.getMostSignificantBits());
        out.writeLong(this.uuid.getLeastSignificantBits());
    }
}
