package pl.kiosel.playerlist.command;

import pl.kiosel.playerlist.AdvancedPlayerList;
import pl.kiosel.playerlist.command.adminsubcommand.*;

import java.util.List;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import pl.kiosel.rosacore.command.RosaCommand;

public class AdminCommand extends RosaCommand {

    public AdminCommand(AdvancedPlayerList plugin) {
        super(plugin, "advancedplayerlist", Arrays.asList("playerlist", "tablist", "tab"));
		setPermission("advancedplayerlist.command.main");
		setDescription("Admin command for AdvancedPlayerList");
		setUsage("/advancedplayerlist");

		addSubCommand(new EnableSubCommand(plugin));
		addSubCommand(new DisableSubCommand(plugin));
		addSubCommand(new ReloadSubCommand(plugin));
		addSubCommand(new ImportSubCommand(plugin));
		addSubCommand(new PlaceholderSubCommand(plugin));
		addSubCommand(new CheckSubCommand(plugin));
		addSubCommand(new SizeSubCommand(plugin));
		addSubCommand(new DiagSubCommand(plugin));
		addSubCommand(new ProfileSubCommand(plugin));
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
			sendHelp(sender, label);
            return true;
        }
		return true;
    }
	
	@Override
	public List<String> onTabComplete(CommandSender commandSender, String[] args) {
		return EMPTY;
	}
}
