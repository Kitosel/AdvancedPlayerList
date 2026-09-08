package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.api.TablistProfileInfo;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ProfileSubCommand extends RosaSubCommand {

	private static final List<String> ACTIONS = Arrays.asList("list", "status", "activate", "restore");
	private final AdvancedPlayerList plugin;

	public ProfileSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "profile";
	}

	@Override
	public String getDescription() {
		return "Manage tablist profiles";
	}

	@Override
	public String getUsage() {
		return "/tab profile <list|status|activate|restore>";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.profile";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		if (args.length == 0) {
			getMessage().sendPrefixed(sender, Lang.PROFILE_USAGE);
			return;
		}

		String action = args[0];
		if (action.equalsIgnoreCase("list") && args.length == 1) {
			List<String> profiles = new ArrayList<>();
			for (TablistProfileInfo profile : plugin.getProfileManager().getProfiles()) {
				profiles.add((profile.isActive() ? "&a" : "&7") + profile.getId()
						+ " &8(" + profile.getOwner() + ")");
			}
			getMessage().sendPrefixed(sender, Lang.PROFILE_LIST,
					"profiles", String.join("&8, ", profiles));
			return;
		}

		if (action.equalsIgnoreCase("status") && args.length == 1) {
			getMessage().sendPrefixed(sender, Lang.PROFILE_STATUS,
					"active", plugin.getProfileManager().getActiveProfile(),
					"previous", plugin.getProfileManager().getPreviousProfile());
			return;
		}

		if (action.equalsIgnoreCase("activate") && args.length == 2) {
			try {
				plugin.getProfileManager().activate(args[1]);
				getMessage().sendPrefixed(sender, Lang.PROFILE_ACTIVATED,
						"profile", plugin.getProfileManager().getActiveProfile());
			} catch (RuntimeException exception) {
				getMessage().sendPrefixed(sender, Lang.PROFILE_ERROR, "error", exception.getMessage());
			}
			return;
		}

		if (action.equalsIgnoreCase("restore") && args.length == 1) {
			try {
				String profile = plugin.getProfileManager().restore();
				getMessage().sendPrefixed(sender, Lang.PROFILE_RESTORED, "profile", profile);
			} catch (RuntimeException exception) {
				getMessage().sendPrefixed(sender, Lang.PROFILE_ERROR, "error", exception.getMessage());
			}
			return;
		}

		getMessage().sendPrefixed(sender, Lang.PROFILE_USAGE);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			return complete(args[0], ACTIONS);
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("activate")) {
			List<String> profiles = new ArrayList<>();
			for (TablistProfileInfo profile : plugin.getProfileManager().getProfiles()) {
				profiles.add(profile.getId());
			}
			return complete(args[1], profiles);
		}
		return EMPTY;
	}
}
