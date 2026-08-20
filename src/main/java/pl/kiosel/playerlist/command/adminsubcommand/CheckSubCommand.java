package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.util.Utils;
import pl.kiosel.rosacore.command.RosaSubCommand;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.Objects;

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
			sender.sendMessage("PlaceholderApi is not installed!");
			return;
		}
		try {
			int count = 0;
			for (String identifier : Objects.requireNonNull(PlaceholderManager.getRegisteredPlaceholderIdentifiers())) {
				count++;
				sender.sendMessage(ColorUtils.color("&e * " + identifier));
			}
			getMessage().sendPrefixed(sender, Lang.CHECK_PLACEHOLDERS, "count", count);
		} catch (Throwable t) {
			getMessage().sendPrefixed(sender, Lang.REQUIRE_PLACEHOLDER);
		}
		getMessage().sendPrefixed(sender, Lang.MISSING_PLACEHOLDERS, "missing", String.join(", ", PlaceholderManager.missingPlaceholders));
	}
}
