package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class ReloadSubCommand extends RosaSubCommand {

	public ReloadSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String getDescription() {
		return "Reloading configs";
	}

	@Override
	public String getUsage() {
		return "/tab reload";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.reload";
	}

	@Override
	public void run(CommandSender sender, String[] strings) {
		plugin.reloadRosaConfig();
		getMessage().sendPrefixed(sender, Lang.RELOAD);
	}
}
