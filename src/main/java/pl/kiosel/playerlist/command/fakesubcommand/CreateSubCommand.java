package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

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
		return "/fakeplayer create <name>";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.create";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(getUsage());
			return;
		}

		if (args.length > 0) {
			String name = args[0];
			if (name.length() > 16) {
				getMessage().sendPrefixed(sender, Lang.MAX_CHARACTERS);
				return;
			}
			if (plugin.getPlayerBank().getFakePlayer(name) != null) {
				getMessage().sendPrefixed(sender, Lang.ALREADY_EXISTS, "name", name);
				return;
			}
			plugin.getPlayerBank().createFakePlayer(name);
			getMessage().sendPrefixed(sender, Lang.SPAWN);
		}
	}
}
