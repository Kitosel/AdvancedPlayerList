package pl.kiosel.playerlist.command;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import pl.kiosel.playerlist.command.fakesubcommand.CreateSubCommand;
import pl.kiosel.playerlist.command.fakesubcommand.DeleteSubCommand;
import pl.kiosel.playerlist.command.fakesubcommand.EditSubCommand;
import pl.kiosel.playerlist.command.fakesubcommand.ListSubCommand;
import org.bukkit.command.CommandSender;
import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.rosacore.command.RosaCommand;

public class FakePlayerCommand extends RosaCommand {

    public FakePlayerCommand(AdvancedPlayerList plugin) {
        super(plugin, "fakeplayer", Arrays.asList("fplayer", "fp", "fakep"));
        setPermission("advancedplayerlist.manage.fakeplayer");
        setDescription("Manage fake players on tablist");

        addSubCommand(new CreateSubCommand(plugin));
        addSubCommand(new DeleteSubCommand(plugin));
        addSubCommand(new EditSubCommand(plugin));
        addSubCommand(new ListSubCommand(plugin));
    }

    @Override
    public boolean onExecute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        sendHelp(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] args) {
        return EMPTY;
    }

}
