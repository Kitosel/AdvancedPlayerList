package pl.kiosel.playerlist.internal;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import org.bukkit.GameMode;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.model.skin.SkinToolkit;
import org.bukkit.entity.Player;
import java.util.Objects;
import pl.kiosel.playerlist.placeholder.ComplexSortable;
import java.util.ArrayList;

import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.playerlist.tablist.TablistLayout;
import pl.kiosel.playerlist.tablist.TablistLayoutHandler;
import pl.kiosel.rosacore.method.Grouping;

import java.util.List;

public class InternalLayoutHandler extends TablistLayoutHandler {

    public InternalLayoutHandler(TablistLayoutHandler parent) {
        super(parent);
    }

    public static String replaceFirst(String text, String target, String replacement) {
        int index = text.indexOf(target);
        if (index >= 0) {
            String before = text.substring(0, index);
            String after = text.substring(index + target.length());
            return before + replacement + after;
        }
        return text;
    }
    
    public List<ComplexPlaceholder> getSortedComplexPlaceholders() {
        List<ComplexPlaceholder> complexes = new ArrayList<>(PlaceholderManager.getComplexPlaceholders());
        complexes.sort((a, b) -> {
            if (a instanceof ComplexSortable) {
                if (b instanceof ComplexSortable) {
                    String groupA = ((ComplexSortable)a).getGroup();
                    String groupB = ((ComplexSortable)b).getGroup();

                    return Objects.equals(groupA, groupB) ?
                            Integer.compare(((ComplexSortable)b).getPriority(), ((ComplexSortable)a).getPriority()) : 0;
                } else {
                    return -1;
                }
            } else if (b instanceof ComplexSortable) {
                return 1;
            } else {
                return 0;
            }
        });
        return complexes;
    }
    
    @Override
    public void handle(Player viewer, TablistLayout layout) {
        Grouping grouping = new Grouping();
        for (int i = 0; i < layout.getSize(); ++i) {
            LineData ld = layout.getLine(i);
            if (layout.getDefaultDisplay() != null) {
                ld.setText(this.replace(viewer, layout.getDefaultDisplay()));
            }
            if (layout.getDefaultPing() != null) {
                ld.setPing(this.parseInt(this.replace(viewer, layout.getDefaultPing())));
            }
            if (layout.getDefaultSkin() != null) {
                ld.setSkin(SkinToolkit.getDefaultToolkit().getSkinPredicate(this.replace(viewer, layout.getDefaultSkin())));
            }
        }
        List<ComplexPlaceholder> placeholders = this.getSortedComplexPlaceholders();
        boolean tryAgain;
        do {
            tryAgain = false;
            for (ComplexPlaceholder complex : placeholders) {
                int max = layout.countPlaceholders(complex.name());
                if (max == 0) {
                    continue;
                }
                String placeholder = "{" + complex.name() + "}";
                ComplexSession session = complex.newSession(viewer, grouping);
                int index = 0;
                String overflow = complex.getOverflowFormat();

                for (int j = 0; j < layout.getSize(); ++j) {
                    LineData ld2 = layout.getLine(j);
                    String text = ld2.getText();

                    if (text != null) {
                        while (true) {
                            int remaining = session.getSize() - index;
                            int left = max - index;
                            if (text == null || !text.contains(placeholder) || remaining <= 0 || left <= 0) {
                                break;
                            }
                            if (left <= 1 && remaining > 1 && overflow != null) {
                                text = PlaceholderManager.replace(overflow, new ExtraData()
                                        .put(ExtraData.DATA_PLAYER, viewer)
                                        .put(ExtraData.DATA_VIEWER, viewer)
                                        .put(ExtraData.DATA_OVERFLOWCOUNT, remaining));
                            } else {
                                ExtraData data = session.getValue(index);
                                if (data == null) {
                                    data = new ExtraData();
                                }
                                String replacement = data.get(ExtraData.DATA_TEXT, "");

                                text = replaceFirst(text, placeholder, replacement);
                                data.ifPresent(ExtraData.DATA_PING, ping ->
                                        ld2.setPing(this.parseInt(this.replace(viewer, ping.toString()))));
                                data.ifPresent(ExtraData.DATA_SKIN, skin ->
                                        ld2.setSkin(SkinToolkit.getDefaultToolkit().getSkinPredicate(
                                                this.replace(viewer, skin.toString()))));

                                Object opacity = data.get(ExtraData.DATA_OPACITY);
                                if (opacity != null && applyOpacity(viewer, ld2, opacity)) {
                                    text = "";
                                }
                                ++index;
                            }
                        }
                        if (text != null) {
                            text = replaceFirst(text, placeholder, "");
                        }
                    }
                    ld2.setText(text);
                }
                if (index <= 0) {
                    continue;
                }
                tryAgain = true;
            }
        } while (tryAgain);

        ExtraData data2 = new ExtraData().put(ExtraData.DATA_VIEWER, viewer).put(ExtraData.DATA_PLAYER, viewer);
        for (int k = 0; k < layout.getSize(); ++k) {
            LineData text2 = layout.getLine(k);
            text2.setText(PlaceholderManager.replace(text2.getText(), data2));
        }
    }
    
    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean applyOpacity(Player viewer, LineData line, Object opacity) {
        String value = this.replace(viewer, opacity.toString()).toLowerCase();
        switch (value) {
            case "invisible":
                return true;
            case "half":
                line.setGameMode(GameMode.SPECTATOR);
                return false;
            case "visible":
                line.setGameMode(GameMode.CREATIVE);
                return false;
            default:
                try {
                    line.setGameMode(GameMode.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
                return false;
        }
    }
    
    private String replace(Player viewer, String text) {
        return PlaceholderManager.replace(text, new ExtraData().put(ExtraData.DATA_VIEWER, viewer));
    }
}
