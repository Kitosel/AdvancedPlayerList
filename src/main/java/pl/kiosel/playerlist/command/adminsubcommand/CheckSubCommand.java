package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.Set;

public class CheckSubCommand extends RosaSubCommand {

	public CheckSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
	}

	@Override
	public String getName() {
		return "check";
	}

	@Override
	public String getDescription() {
		return "Checking installed placeholders";
	}

	@Override
	public String getUsage() {
		return "/tab check";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.check";
	}

	@Override
	public void run(CommandSender sender, String[] strings) {
		if (!AdvancedPlayerList.isPlaceholderAPI()) {
			getMessage().sendPrefixed(sender, Lang.REQUIRE_PLACEHOLDER);
			return;
		}

		Set<String> installed = PlaceholderManager.getRegisteredPlaceholderIdentifiers();
		Set<String> missing = PlaceholderManager.getMissingPlaceholderAPIPlaceholders();
		getMessage().sendPrefixed(sender, Lang.CHECK_PLACEHOLDERS,
				"count", installed.size(),
				"placeholders", format(installed));
		getMessage().sendPrefixed(sender, Lang.MISSING_PLACEHOLDERS,
				"missing", format(missing));
	}

	private String format(Set<String> placeholders) {
		return placeholders.isEmpty() ? "-" : String.join(", ", placeholders);
	}
}
