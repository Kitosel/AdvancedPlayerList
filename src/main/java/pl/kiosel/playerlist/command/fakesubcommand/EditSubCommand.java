package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;
import pl.kiosel.rosacore.utils.CommandUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class EditSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public EditSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "edit";
	}

	@Override
	public String getDescription() {
		return "Edit a fake player";
	}

	@Override
	public String getUsage() {
		return "/fakeplayer edit <name> <addplaceholder/removeplaceholder> <placeholder> [value]";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.edit";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(getUsage());
			return;
		}

		String name = args[0];
		FakePlayer dp = plugin.getPlayerBank().getFakePlayer(name);
		if (dp == null) {
			getMessage().sendPrefixed(sender, Lang.NOT_EXISTS, "name", name);
			return;
		}

		if (args.length > 1) {
			if (args[1].equalsIgnoreCase("addplaceholder")) {
				if (args.length > 3) {
					String text = args[2];
					String value = joinArguments(args, 3);
					dp.placeholders().put(text, value);
					getMessage().sendPrefixed(sender, Lang.ADDED_PLACEHOLDER, "text", text, "name", name, "value", value);
					return;
				}
			} else if (args[1].equalsIgnoreCase("removeplaceholder")) {
				if (args.length > 2) {
					String text = args[2];
					dp.placeholders().remove(text);
					getMessage().sendPrefixed(sender, Lang.REMOVED_PLACEHOLDER, "text", text, "name", name);
					return;
				}
			}
		}
		sender.sendMessage(getUsage());
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
		if (args.length == 2) {
			return CommandUtils.returnWith(args[1], Arrays.asList("addplaceholder", "removeplaceholder"));
		}
		if (args.length == 3 && "removeplaceholder".equalsIgnoreCase(args[1])) {
			FakePlayer player = plugin.getPlayerBank().getFakePlayer(args[0]);
			if (player != null) {
				return CommandUtils.returnWith(args[2], new ArrayList<>(player.placeholders().keySet()));
			}
		}
		return EMPTY;
	}

	static String joinArguments(String[] args, int startIndex) {
		StringJoiner value = new StringJoiner(" ");
		for (int index = Math.max(0, startIndex); index < args.length; index++) {
			value.add(args[index]);
		}
		return value.toString();
	}
}
