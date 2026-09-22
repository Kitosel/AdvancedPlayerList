package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.model.PlayerBank;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.ArrayList;
import java.util.List;

public final class RefreshSkinSubCommand extends RosaSubCommand {

    private final AdvancedPlayerList plugin;

    public RefreshSkinSubCommand(AdvancedPlayerList plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "refreshskin";
    }

    @Override
    public String getDescription() {
        return "Refresh a fake player's skin";
    }

    @Override
    public String getUsage() {
        return "/fakeplayer refreshskin <name>";
    }

    @Override
    public String getPermission() {
        return "advancedplayerlist.command.refreshskin";
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return;
        }
        FakePlayer player = plugin.getPlayerBank().getFakePlayer(args[0]);
        if (player == null) {
            getMessage().sendPrefixed(sender, Lang.NOT_EXISTS, "name", args[0]);
            return;
        }

        getMessage().sendPrefixed(sender, Lang.SKIN_REFRESHING,
                "name", player.getName(), "skin", player.describeSkinSource());
        plugin.getPlayerBank().refreshSkin(player.getName(), result -> {
            if (result == PlayerBank.SkinLookupResult.NOT_FOUND
                    || result == PlayerBank.SkinLookupResult.INVALID) {
                FakePlayerCommandSupport.reportLookupFailure(
                        getMessage(), sender, player, result);
                return;
            }
            FakePlayerCommandSupport.runSync(sender, () -> getMessage().sendPrefixed(sender,
                    result == PlayerBank.SkinLookupResult.DISABLED
                            ? Lang.SKIN_DISABLED : Lang.SKIN_REFRESHED,
                    "name", player.getName(), "skin", player.describeSkinSource()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return EMPTY;
        }
        List<String> names = new ArrayList<>();
        for (FakePlayer player : plugin.getPlayerBank().getFakePlayers()) {
            names.add(player.getName());
        }
        return complete(args[0], names);
    }
}
