package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.model.skin.SkinToolkit;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.*;

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
		return "/fakeplayer edit <name> <head <yes|no>|skin <auto|none|name|uuid|url>|"
				+ "setplaceholder <placeholder> <value>|removeplaceholder <placeholder>>";
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

		if (args.length < 2) {
			sendUsage(sender);
			return;
		}

		String action = args[1].toLowerCase(Locale.ROOT);
		switch (action) {
			case "head": {
				if (args.length != 3) {
					sendUsage(sender);
					return;
				}
				Boolean enabled = FakePlayerCommandSupport.parseEnabled(args[2]);
				if (enabled == null) {
					getMessage().sendPrefixed(sender, Lang.INVALID_BOOLEAN, "value", args[2]);
					return;
				}
				dp.setRenderHead(enabled);
				plugin.getPlayerBank().savePlayerBank();
				getMessage().sendPrefixed(sender, Lang.HEAD_UPDATED,
						"name", name, "head", enabled ? "enabled" : "disabled");
				if (enabled) {
					plugin.getPlayerBank().resolveSkin(dp, false, result ->
							FakePlayerCommandSupport.reportLookupFailure(
									getMessage(), sender, dp, result));
				}
				return;
			}
			case "skin": {
				if (args.length != 3) {
					sendUsage(sender);
					return;
				}
				String skin = args[2];
				if (!SkinToolkit.getDefaultToolkit().isValidSkinReference(skin)) {
					getMessage().sendPrefixed(sender, Lang.INVALID_SKIN, "name", name, "skin", skin);
					return;
				}
				dp.setSkinSource(skin);
				plugin.getPlayerBank().savePlayerBank();
				getMessage().sendPrefixed(sender, Lang.SKIN_UPDATED,
						"name", name, "skin", dp.describeSkinSource());
				plugin.getPlayerBank().resolveSkin(dp, true, result ->
						FakePlayerCommandSupport.reportLookupFailure(
								getMessage(), sender, dp, result));
				return;
			}
			case "setplaceholder": {
				if (args.length < 4) {
					sendUsage(sender);
					return;
				}
				String text = args[2];
				String value = joinArguments(args, 3);
				dp.placeholders().put(text, value);
				plugin.getPlayerBank().savePlayerBank();
				getMessage().sendPrefixed(sender, Lang.ADDED_PLACEHOLDER,
						"text", text, "name", name, "value", value);
				return;
			}
			case "removeplaceholder": {
				if (args.length != 3) {
					sendUsage(sender);
					return;
				}
				String text = args[2];
				dp.placeholders().remove(text);
				plugin.getPlayerBank().savePlayerBank();
				getMessage().sendPrefixed(sender, Lang.REMOVED_PLACEHOLDER,
						"text", text, "name", name);
				return;
			}
			default:
				sendUsage(sender);
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sendUsage(sender);
		}
		if (args.length == 1) {
			List<String> names = new ArrayList<>();
			for (FakePlayer player : plugin.getPlayerBank().getFakePlayers()) {
				names.add(player.getName());
			}
			return complete(args[0], names);
		}
		if (args.length == 2) {
			return complete(args[1], Arrays.asList("setplaceholder", "removeplaceholder", "skin", "head"));
		}
		if (args.length == 3) {
			switch (args[1].toLowerCase(Locale.ROOT)) {
				case "skin":
					List<String> skins = new ArrayList<>(Arrays.asList("auto", "none"));
					skins.addAll(onlinePlayers());
					return complete(args[2], skins);
				case "head":
					return complete(args[2], Arrays.asList("enable", "disable"));
				case "removeplaceholder":
					FakePlayer player = plugin.getPlayerBank().getFakePlayer(args[0]);
					if (player != null) {
						return complete(args[2], new ArrayList<>(player.placeholders().keySet()));
					}
					break;
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
