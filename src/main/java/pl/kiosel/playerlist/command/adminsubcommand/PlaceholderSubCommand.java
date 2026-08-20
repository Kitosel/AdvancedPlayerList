package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.placeholder.ExtraData;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.Arrays;

import static pl.kiosel.playerlist.placeholder.ExtraData.DATA_PLAYER;
import static pl.kiosel.playerlist.placeholder.ExtraData.DATA_VIEWER;

public class PlaceholderSubCommand extends RosaSubCommand {

	public PlaceholderSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "placeholder";
	}

	@Override
	public String getDescription() {
		return "Replace placeholders";
	}

	@Override
	public String getUsage() {
		return "/tab placeholder <text/placeholder>";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.placeholder";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length > 0) {
			String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
			ExtraData data = (sender instanceof Player) ? new ExtraData().put(DATA_PLAYER, sender).put(DATA_VIEWER, sender) : new ExtraData();
			getMessage().sendPrefixed(sender, Lang.PLACEHOLDER_TEST, "message", message, "placeholder", PlaceholderManager.replace(message, data));
		}
	}
}
