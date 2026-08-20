package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class EnableSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public EnableSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "enable";
	}

	@Override
	public String getDescription() {
		return "Enabling tablist";
	}

	@Override
	public String getUsage() {
		return "/tab enable";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.enable";
	}

	@Override
	public void run(CommandSender sender, String[] strings) {
		if (plugin.getTablistManager().isTablistEnabled()) {
			getMessage().sendPrefixed(sender, Lang.TABLIST_ALREADY_ENABLED);
			return;
		}
		plugin.getTablistManager().setTablistEnabled(true);
		getMessage().sendPrefixed(sender, Lang.TABLIST_ENABLE);
	}
}
