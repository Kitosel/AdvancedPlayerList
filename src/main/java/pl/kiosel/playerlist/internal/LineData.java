package pl.kiosel.playerlist.internal;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.model.skin.Skin;
import org.bukkit.GameMode;

import java.io.Externalizable;

public class LineData implements Externalizable {

    @Getter @Setter private GameMode gameMode;
    @Setter @Getter private String text;
    @Setter @Getter private Skin skin;
    @Setter private Integer ping;
    private boolean hideEmpty;
    
    public LineData() {
    }
    
    public LineData(String text) {
        this.text = text;
    }

    @Override
    public LineData clone() {
        LineData data = new LineData();
        data.text = this.text;
        data.ping = this.ping;
        data.skin = this.skin;
        data.gameMode = this.gameMode;
        data.hideEmpty = this.hideEmpty;
        return data;
    }

	public void hideEmpty() {
        this.hideEmpty = true;
    }
    
    public void keepShow() {
        this.hideEmpty = false;
    }

	public boolean shouldHideEmpty() { return this.hideEmpty; }
    public int getPing() { return (this.ping == null) ? 0 : this.ping; }
	public boolean hasPingSet() { return this.ping != null; }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.text = in.readUTF();
        this.ping = (Integer)in.readObject();
        (this.skin = new Skin()).readExternal(in);
        this.hideEmpty = in.readBoolean();
    }

	@Override
    public String toString() {
        return "LineData[text=" + this.text + ",ping=" + this.ping + ",skin=" + this.skin + "]";
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.text);
        out.writeObject(this.ping);
        this.skin.writeExternal(out);
        out.writeBoolean(this.hideEmpty);
    }
    
    public static class Builder {
        private String text;
        private Integer ping;
        private Skin skin;
        
        public Builder() {
        }
        
        public Builder(String text) {
            this.text = text;
        }
        
        public Builder(String text, int ping) {
            this(text);
            this.ping = ping;
        }
        
        public Builder(String text, int ping, Skin skin) {
            this(text, ping);
            this.skin = skin;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder ping(int ping) {
            this.ping = ping;
            return this;
        }
        
        public Builder skin(Skin skin) {
            this.skin = skin;
            return this;
        }
        
        public LineData build() {
            LineData data = new LineData(this.text);
            data.setPing(this.ping);
            data.setSkin(this.skin);
            return data;
        }
    }
}
