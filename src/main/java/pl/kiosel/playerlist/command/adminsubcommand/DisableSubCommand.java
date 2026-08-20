package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class DisableSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public DisableSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "disable";
	}

	@Override
	public String getDescription() {
		return "Disabling tablist";
	}

	@Override
	public String getUsage() {
		return "/tab disable";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.disable";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (!plugin.getTablistManager().isTablistEnabled()) {
			getMessage().sendPrefixed(sender, Lang.TABLIST_ALREADY_DISABLED);
			return;
		}
		plugin.getTablistManager().setTablistEnabled(false);
		getMessage().sendPrefixed(sender, Lang.TABLIST_DISABLE);
	}
}
