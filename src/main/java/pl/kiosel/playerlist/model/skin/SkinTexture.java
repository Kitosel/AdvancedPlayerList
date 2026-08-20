package pl.kiosel.playerlist.model.skin;

import lombok.Getter;

import java.io.ObjectOutput;
import java.util.Base64;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Objects;
import java.net.URL;
import java.io.Externalizable;

public class SkinTexture implements Externalizable {

    @Getter protected String value;
    @Getter protected String signature;
    protected URL url;
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SkinTexture) {
            final SkinTexture other = (SkinTexture)obj;
            return Objects.equals(this.value, other.value)
                    && Objects.equals(this.signature, other.signature);
        }
        return false;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        this.value = in.readUTF();
        this.signature = in.readUTF();
        this.url = (URL)in.readObject();
    }
    
    protected String toJson() {
        return "{\"value\":\"" + new String(Base64.getDecoder().decode(this.value)) +
                "\", \"signature\":\"" + this.signature +
                "\",\"url\":\"" + this.url + "\"}";
    }
    
    @Override
    public String toString() {
        return "Texture[value=" + this.value + ",signature=" + this.signature + ",url=" + this.url + "]";
    }
    
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeUTF(this.value);
        out.writeUTF(this.signature);
        out.writeObject(this.url);
    }
}
