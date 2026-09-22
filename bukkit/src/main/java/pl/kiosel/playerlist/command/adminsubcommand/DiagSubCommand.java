package pl.kiosel.playerlist.command.adminsubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.bridge.BridgeClient;
import pl.kiosel.playerlist.model.Diagnostics;
import pl.kiosel.playerlist.model.Evaluator;
import pl.kiosel.playerlist.model.skin.SkinToolkit;
import pl.kiosel.playerlist.protocol.Protocol;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;
import pl.kiosel.rosacore.utils.ReflectionUtils;
import pl.kiosel.rosacore.version.Version;

import java.util.List;

public class DiagSubCommand extends RosaSubCommand {

	private final AdvancedPlayerList plugin;

	public DiagSubCommand(AdvancedPlayerList plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "diag";
	}

	@Override
	public String getDescription() {
		return "Diagnosing and Checking for Errors in the Configuration";
	}

	@Override
	public String getUsage() {
		return "/tab diag";
	}

	@Override
	public String getPermission() {
		return "advancedplayerlist.command.diag";
	}

	@Override
	public void run(CommandSender sender, String[] args) {
		sendOptionalMessage(sender, "&8&m--------------------------------");
		sendOptionalMessage(sender, "&6AdvancedPlayerList &fv" + plugin.getDescription().getVersion());

		sendOptionalMessage(sender, "&fServer: &e"
				+ Version.getServerVersion()
				+ " &7(" + (Protocol.usesModernPlayerInfo() ? "modern" : "legacy") + ")");

		sendOptionalMessage(sender, "&fJava: &e" + ReflectionUtils.JAVA_VERSION);
		sendOptionalMessage(sender, "&fPackets: &eRosaCore NMS");
		sendOptionalMessage(sender, "&fNMS: &e" + Version.getServerRevisionVersion());
		sendOptionalMessage(sender, "&fScripts: &e" + Evaluator.getEngineSource());
		sendOptionalMessage(sender, "&fActive profile: &e" + plugin.getProfileManager().getActiveProfile());

		int renderedHeads = 0;
		for (FakePlayer player : plugin.getPlayerBank().getFakePlayers()) {
			if (player.isRenderHead() && !"none".equalsIgnoreCase(player.getSkinSource())) {
				renderedHeads++;
			}
		}
		sendOptionalMessage(sender, "&fFake players: &e" + plugin.getPlayerBank().getFakePlayerCount()
				+ " &7(heads: " + renderedHeads + ", storage v"
				+ plugin.getPlayerBank().getStorageFormatVersion() + ")");

		SkinToolkit skins = SkinToolkit.getDefaultToolkit();
		sendOptionalMessage(sender, "&fSkins: &a" + skins.getCachedSkinCount() + " cached"
				+ "&7, &e" + Math.max(skins.getPendingSkinCount(),
				plugin.getPlayerBank().getPendingSkinLookupCount()) + " pending"
				+ "&7, &c" + Math.max(skins.getUnavailableAccountCount(),
				plugin.getPlayerBank().getUnavailableSkinLookupCount()) + " unavailable");

		BridgeClient bridge = plugin.getBridgeClient();
		String bridgeStatus = bridge == null || !bridge.isEnabled()
				? "disabled" : bridge.isConnected() ? "connected" : "waiting";
		sendOptionalMessage(sender, "&fBridge: &e" + bridgeStatus);

		List<String> problems = Diagnostics.inspect(plugin);
		if (problems.isEmpty()) {
			sendOptionalMessage(sender, "&aNo problems detected.");
		} else {
			sendOptionalMessage(sender, "&cProblems detected: &e" + problems.size());
			int limit = Math.min(8, problems.size());
			for (int index = 0; index < limit; index++) {
				sendOptionalMessage(sender, "&c- &e" + problems.get(index));
			}
			if (problems.size() > limit) {
				sendOptionalMessage(sender, "&7... and " + (problems.size() - limit)
						+ " more; check the console.");
			}
		}

		sendOptionalMessage(sender, "&8&m--------------------------------");
	}
}
