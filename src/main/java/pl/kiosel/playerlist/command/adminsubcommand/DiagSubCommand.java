package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.command.RosaSubCommand;

public class DiagSubCommand extends RosaSubCommand {

	public DiagSubCommand(RosaPlugin plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "diag";
	}

	@Override
	public String getDescription() {
		return "Diagnosing and Checking for Errors in the Configuration";
	}

	@Override
	public String getUsage() {
		return "/tab diag";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.diag";
	}

	@Override
	public void run(CommandSender commandSender, String[] strings) {

	}
}
