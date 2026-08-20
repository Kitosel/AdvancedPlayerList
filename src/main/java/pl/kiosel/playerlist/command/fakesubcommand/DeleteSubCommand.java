package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;
import pl.kiosel.rosacore.utils.CommandUtils;

import java.util.ArrayList;
import java.util.List;

public class DeleteSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public DeleteSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "delete";
	}

	@Override
	public String getDescription() {
		return "Delete fake player";
	}

	@Override
	public String getUsage() {
		return "/fakeplayer delete <name>";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.delete";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(getUsage());
			return;
		}

		String name = args[0];
		if (this.plugin.getPlayerBank().deleteFakePlayer(name) == null) {
			getMessage().sendPrefixed(sender, Lang.NOT_EXISTS, "name", name);
		} else {
			getMessage().sendPrefixed(sender, Lang.REMOVED, "name", name);
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			List<String> names = new ArrayList<>();
			for (FakePlayer player : plugin.getPlayerBank().getFakePlayers()) {
				names.add(player.getName());
			}
			return CommandUtils.returnWith(args[0], names);
		}
		return EMPTY;
	}
}
