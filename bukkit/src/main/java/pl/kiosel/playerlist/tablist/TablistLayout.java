package pl.kiosel.playerlist.tablist;

import lombok.Getter;
import lombok.Setter;
import pl.kiosel.playerlist.internal.LineData;

import java.util.List;

public class TablistLayout {
    
    @Getter
	@Setter
	private String defaultPing;
    @Getter
	@Setter
	private String defaultSkin;
    @Getter
	@Setter
	private String defaultDisplay;
    private LineData[][] data;
    @Getter
	private int size;
    private int cols;
    private int lens;
    
    public static int columns(int size) {
        return (size + 19) / 20;
    }
    
    public static int lines(int size) {
        final int cols = columns(size);
        return (size + cols - 1) / cols;
    }
    
    private TablistLayout() {
        this.defaultPing = "0";
        this.defaultSkin = null;
        this.defaultDisplay = null;
    }
    
    public TablistLayout(int size) {
        this.defaultPing = "0";
        this.defaultSkin = null;
        this.defaultDisplay = null;
        this.data = new LineData[4][20];
        this.setSize(size);
        this.fill(java.util.Collections.<String>emptyList());
    }
    
    public TablistLayout(int size, final List<String> lines) {
        this.defaultPing = "0";
        this.defaultSkin = null;
        this.defaultDisplay = null;
        this.data = new LineData[4][20];
        this.setSize(size);
        this.fill(lines);
    }
    
    public TablistLayout(List<LineData> lines, int size, boolean[] hide) {
        this.defaultPing = "0";
        this.defaultSkin = null;
        this.defaultDisplay = null;
        this.data = new LineData[4][20];
        this.setSize(size);
        this.fillLines(lines, hide);
    }
    
    public TablistLayout clone() {
        TablistLayout layout = new TablistLayout();
        layout.data = new LineData[4][20];
        layout.setSize(this.size);
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 20; ++j) {
                LineData line = this.data[i][j];
                layout.data[i][j] = line == null ? new LineData(this.defaultDisplay) : line.clone();
            }
        }
        layout.defaultPing = this.defaultPing;
        layout.defaultSkin = this.defaultSkin;
        layout.defaultDisplay = this.defaultDisplay;
        return layout;
    }
    
    public int countPlaceholders(String val) {
        int total = 0;
        for (int i = 0; i < this.getSize(); ++i) {
            String str = this.getLine(i).getText();
            if (str != null) {
                while (true) {
                    int index = str.indexOf("{" + val + "}");
                    if (index < 0) {
                        break;
                    }
                    str = str.substring(index + val.length() + 2);
                    ++total;
                }
            }
        }
        return total;
    }
    
    public void fill(List<String> lines) {
        for (int i = 0; i < 80; ++i) {
            if (i >= lines.size()) {
                this.data[i / 20][i % 20] = new LineData(this.defaultDisplay);
            } else {
                this.data[i / 20][i % 20] = new LineData(lines.get(i));
            }
        }
    }
    
    public void fillLines(List<LineData> lines, boolean[] hide) {
        for (int i = 0; i < 80; ++i) {
            if (i >= lines.size()) {
                this.data[i / 20][i % 20] = new LineData(this.defaultDisplay);
            } else {
                this.data[i / 20][i % 20] = lines.get(i);
            }
            if (hide[i / 20]) {
                this.data[i / 20][i % 20].hideEmpty();
            }
        }
    }

	public void setLine(int index, LineData line) {
        this.data[index / this.lens][index % this.lens] = line;
    }
    
    public LineData getLine(int index) {
        return this.data[index / this.lens][index % this.lens];
    }
    
    public LineData[][] getLines() {
        return this.data;
    }

	public void setSize(int size) {
        int normalized = Math.min(80, Math.max(20, ((size + 19) / 20) * 20));
        this.size = normalized;
        this.cols = columns(normalized);
        this.lens = lines(normalized);
    }
}
