package pl.kiosel.playerlist.model.skin;

import lombok.Getter;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Objects;
import java.util.UUID;
import java.io.Externalizable;

public class Skin implements Externalizable {

    protected String key;
    protected volatile UUID uuid;
    @Getter
    protected volatile SkinTexture texture;
    
    @Override
    public boolean equals(Object other) {
        return other instanceof Skin && Objects.equals(this.key, ((Skin)other).key);
    }
    
    protected void setKey(Object obj) {
        this.key = String.valueOf(obj);
    }

	public UUID getUniqueId() {
        return this.uuid;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.key);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readBoolean()) {
            this.uuid = new UUID(in.readLong(), in.readLong());
        }
        if (in.readBoolean()) {
            (this.texture = new SkinTexture()).readExternal(in);
        }
    }
    
    public String toJson() {
        return "{\"uuid\":\"" + this.uuid + "\",\"texture\":\"" + ((this.texture == null) ? null : this.texture.toJson()) + "\"}";
    }
    
    @Override
    public String toString() {
        return "Skin[uuid=" + this.uuid + ",texture=" + this.texture + "]";
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(this.uuid != null);
        if (this.uuid != null) {
            out.writeLong(this.uuid.getMostSignificantBits());
            out.writeLong(this.uuid.getLeastSignificantBits());
        }
        out.writeBoolean(this.texture != null);
        if (this.texture != null) {
            this.texture.writeExternal(out);
        }
    }
}
