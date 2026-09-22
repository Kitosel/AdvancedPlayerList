package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.Arrays;
import java.util.List;

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
			Boolean requested = FakePlayerCommandSupport.parseEnabled(args[1]);
			if (requested == null) {
				sendUsage(sender);
				return;
			}
			renderHead = requested;
		}
		plugin.getPlayerBank().createFakePlayer(name, renderHead);
		FakePlayer player = plugin.getPlayerBank().getFakePlayer(name);
		plugin.getPlayerBank().savePlayerBank();
		getMessage().sendPrefixed(sender, Lang.SPAWN,
				"name", name,
				"head", renderHead ? "yes" : "no",
				"skin", player == null ? "disabled" : player.describeSkinSource());
		if (player != null) {
			plugin.getPlayerBank().resolveSkin(player, false, result ->
					FakePlayerCommandSupport.reportLookupFailure(getMessage(), sender, player, result));
		}
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
