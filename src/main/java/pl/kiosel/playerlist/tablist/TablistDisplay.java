package pl.kiosel.playerlist.tablist;

import pl.kiosel.playerlist.util.DisplayData;
import org.bukkit.entity.Player;

public class TablistDisplay {
    private final DisplayFinder header;
    private final DisplayFinder footer;
    
    public TablistDisplay(DisplayData[] headers, DisplayData[] footers) {
        this.header = new DisplayFinder(headers);
        this.footer = new DisplayFinder(footers);
    }
    
    public String getCurrentFooter() { return this.footer.getCurrentText(); }
    public String getCurrentHeader() { return this.header.getCurrentText(); }
    
    public void tick(Player player) {
        this.header.tick(player);
        this.footer.tick(player);
    }
    
    public static class DisplayFinder {
        private int index;
        private final DisplayData[] datas;
        
        public DisplayFinder(DisplayData[] array) {
            this.index = 0;
            this.datas = array == null ? new DisplayData[0] : array;
        }
        
        public DisplayData getCurrent() {
            return (this.datas.length == 0) ? null : this.datas[this.index % this.datas.length];
        }
        
        public String getCurrentText() {
            DisplayData current = this.getCurrent();
            return (current == null) ? null : current.getText();
        }

        public void skip(Player player) {
            if (this.datas.length == 0) {
                return;
            }
            for (int i = this.index + 1; i < this.datas.length + this.index + 1; ++i) {
                int id = i % this.datas.length;
                DisplayData data = this.datas[id];
                if (data != null && !data.skip(player)) {
                    data.resetDelay();
                    this.index = id;
                    return;
                }
            }
        }
        
        public void tick(Player player) {
            DisplayData data = this.getCurrent();
            if (data != null) {
                data.setDelay(data.getDelay() - 1);
                if (data.skip(player) || data.getDelay() <= 0) {
                    data.resetDelay();
                    this.skip(player);
                }
            }
        }
    }
}
