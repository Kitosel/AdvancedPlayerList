package pl.kiosel.playerlist.listener;

import pl.kiosel.playerlist.event.TablistLineTextChangeEvent;
import pl.kiosel.playerlist.event.TablistShowEvent;
import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pl.kiosel.playerlist.AdvancedPlayerList;

public class TabListener implements Listener {

    private final AdvancedPlayerList plugin;

    public TabListener(AdvancedPlayerList plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTextChange(TablistLineTextChangeEvent event) {
        int maxLength = plugin.getConfigFile().getInt("slot-length.max-length");
        int minLength = plugin.getConfigFile().getInt("slot-length.min-length");

        String text = event.getText();
        String limiter = plugin.getConfigFile().getString("slot-length.limiter", "...");
        if (text.length() > maxLength) {
            int textLength = Math.max(0, maxLength - limiter.length());
            event.setText(text.substring(0, textLength) + limiter.substring(0,
                    Math.min(limiter.length(), maxLength)));
        } else if (event.getText().length() < minLength) {
            StringBuilder padded = new StringBuilder(event.getText());
            while (padded.length() < minLength) {
                padded.append(' ');
            }
            event.setText(padded.toString());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void listen(TablistShowEvent event) {
        if (event.getTablist().getPlugin() == plugin) {
            Tablist tablist = event.getTablist();
            tablist.setLayoutHandler(plugin.getLayoutHandler());
            tablist.setLayout(plugin.getLayouts().getOrDefault(tablist.getPlayer().getWorld(), plugin.globalLayout));
            tablist.setDisplay(plugin.getDisplays().getOrDefault(tablist.getPlayer().getWorld(), plugin.globalDisplay));
        }
    }
}
