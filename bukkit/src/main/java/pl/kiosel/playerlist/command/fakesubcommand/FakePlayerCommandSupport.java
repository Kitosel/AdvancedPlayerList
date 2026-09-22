package pl.kiosel.playerlist.command.fakesubcommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.playerlist.config.Lang;
import pl.kiosel.playerlist.model.PlayerBank;
import pl.kiosel.playerlist.model.Ticker;
import pl.kiosel.playerlist.util.FakePlayer;
import pl.kiosel.rosacore.message.MessageCatalog;

public final class FakePlayerCommandSupport {

    private FakePlayerCommandSupport() {
    }

    static Boolean parseEnabled(String value) {
        if (value == null) {
            return null;
        }
        switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "true":
            case "yes":
            case "y":
            case "enable":
            case "enabled":
                return Boolean.TRUE;
            case "false":
            case "no":
            case "n":
            case "disable":
            case "disabled":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    static void reportLookupFailure(MessageCatalog messages, CommandSender sender, FakePlayer player,
                                    PlayerBank.SkinLookupResult result) {
        if (result != PlayerBank.SkinLookupResult.NOT_FOUND
                && result != PlayerBank.SkinLookupResult.INVALID) {
            return;
        }
        runSync(sender, () -> messages.sendPrefixed(sender,
                result == PlayerBank.SkinLookupResult.NOT_FOUND ? Lang.SKIN_NOT_FOUND : Lang.INVALID_SKIN,
                "name", player.getName(), "skin", player.describeSkinSource()));
    }

    static void runSync(CommandSender sender, Runnable task) {
        if (sender instanceof Player) {
            Ticker.delay((Player) sender, task, 0L);
        } else {
            Ticker.delay(task, 0L);
        }
    }
}
