package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class ToggleSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public ToggleSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "toggle";
	}

	@Override
	public String getDescription() {
		return "Disable or enable tablist";
	}

	@Override
	public String getUsage() {
		return "/advancedplayerlist toggle";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.toggle";
	}

	@Override
	public boolean isPlayerOnly() {
		return true;
	}

	@Override
	public boolean isAvailable(CommandSender sender) {
		return plugin.getConfigFile().getBoolean("player-can-toggle-tablist");
	}

	@Override
	public void run(CommandSender commandSender, String[] strings) {
		Player player = (Player) commandSender;
		if (plugin.getTablistManager().hasTablistEnabled(player)) {
			plugin.getTablistManager().disableTablist(player);
			getMessage().sendPrefixed(commandSender, Lang.TOGGLE_DISABLE);
		} else {
			plugin.getTablistManager().enableTablist(player);
			getMessage().sendPrefixed(commandSender, Lang.TOGGLE_ENABLE);
		}
	}
}
