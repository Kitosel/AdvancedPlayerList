package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class ImportSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public ImportSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "import";
	}

	@Override
	public String getDescription() {
		return "Importing offlineplayers";
	}

	@Override
	public String getUsage() {
		return "/tab import";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.import";
	}

	@Override
	public void run(CommandSender sender, String[] strings) {
		getMessage().sendPrefixed(sender, Lang.OFFLINEPLAYERS_IMPORT);
		plugin.getOfflinePlayerDatabase().insertBukkit();
		getMessage().sendPrefixed(sender, Lang.OFFLINEPLAYERS_IMPORTED);
	}
}
