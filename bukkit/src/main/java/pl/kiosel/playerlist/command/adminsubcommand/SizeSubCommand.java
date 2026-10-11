package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.config.Settings;
import pl.kiosel.playerlist.tablist.Tablist;
import pl.kiosel.playerlist.tablist.TablistLayout;
import pl.kiosel.rosacore.command.RosaSubCommand;
import pl.kiosel.rosacore.utils.NumberUtils;

import java.util.List;

public class SizeSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public SizeSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "size";
	}

	@Override
	public String getDescription() {
		return "Change tab size";
	}

	@Override
	public String getUsage() {
		return "/tab size <0/1/2/3/4>";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.size";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length > 0) {
			if (!NumberUtils.isInt(args[0])) {
				getMessage().sendPrefixed(sender, Lang.INVALID_NUMBER);
				return;
			}
			int size = NumberUtils.parseInt(args[0], 0);
			if (size>=0 && size<=4) {
				this.toggleColumns(size != 0);
				for (Player player : Bukkit.getOnlinePlayers()) {
					Tablist tablist = plugin.getTablistManager().getTablist(player);
					if (tablist != null && tablist.getPlugin() == plugin) {
						TablistLayout layout = tablist.getLayout().clone();
						layout.setSize(size * 20);
						tablist.setLayout(layout);
					}
				}
				getMessage().sendPrefixed(sender, Lang.CHANGED_SIZE, "size", size);
			}/* else if (size==0) {
				this.toggleColumns(false);
			}*/ else {
				getMessage().sendPrefixed(sender, Lang.INVALID_SIZE, "size", size);
			}
			return;
		}
		int size = 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			Tablist tablist = plugin.getTablistManager().getTablist(player);
			if (tablist != null && tablist.getPlugin() == plugin) {
				TablistLayout layout = tablist.getLayout();
				size = layout.getSize();
				break;
			}
		}
		getMessage().sendPrefixed(sender, Lang.ACTUAL_SIZE, "size", size/20);
	}

	private void toggleColumns(boolean enabled) {
		boolean columns = plugin.getSettings().get(Settings.TABLIST_WITH_COLUMNS);
		if (columns == enabled) {
			return;
		}
		plugin.getConfigFile().set("tablist-with-columns", !columns);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if(args.length == 1) {
			return complete("0", "1", "2", "3", "4");
		}
		return EMPTY;
	}
}
