package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.List;

public class ListSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public ListSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public String getDescription() {
		return "Get a fake players list";
	}

	@Override
	public String getUsage() {
		return "/fakeplayer list";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.list";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		List<FakePlayer> players = plugin.getPlayerBank().getFakePlayers();
		String[] names = new String[players.size()];
		for (int i = 0; i < names.length; ++i) {
			names[i] = players.get(i).getName();
		}
		getMessage().sendPrefixed(sender, Lang.LIST, "number", names.length, "names", String.join(", ", names));
	}
}
