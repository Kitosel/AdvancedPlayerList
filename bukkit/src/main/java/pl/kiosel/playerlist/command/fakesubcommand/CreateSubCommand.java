package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CreateSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public CreateSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "create";
	}

	@Override
	public String getDescription() {
		return "Create a fake player";
	}

	@Override
	public String getUsage() {
		return "/fakeplayer create <name> [render head: yes|no]";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.create";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length == 0 || args.length > 2) {
			sendUsage(sender);
			return;
		}

		String name = args[0];
		if (name.length() > 16) {
			getMessage().sendPrefixed(sender, Lang.MAX_CHARACTERS);
			return;
		}
		if (plugin.getPlayerBank().getFakePlayer(name) != null) {
			getMessage().sendPrefixed(sender, Lang.ALREADY_EXISTS, "name", name);
			return;
		}
		boolean renderHead = true;
		if (args.length == 2) {
			String arg1 = args[1].toLowerCase(Locale.ROOT);
			if (arg1.equals("yes") || arg1.equals("true") || arg1.equals("y")) {
				renderHead = true;
			} else if (arg1.equals("no") || arg1.equals("false") || arg1.equals("n")) {
				renderHead = false;
			} else {
				sendUsage(sender);
				return;
			}
		}
		plugin.getPlayerBank().createFakePlayer(name, renderHead);
		getMessage().sendPrefixed(sender, Lang.SPAWN);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			return complete(args[0], onlinePlayers());
		}
		if (args.length == 2) {
			return complete(args[1], Arrays.asList("yes", "no"));
		}
		return EMPTY;
	}
}
