package pl.kiosel.playerlist.model;

import lombok.Getter;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import pl.kiosel.playerlist.AdvancedPlayerList;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import org.bukkit.World;
import java.util.List;

@Getter
public class WorldGroup {
    private final String name;
    private final List<World> worlds;
    
    public WorldGroup(String name) {
        this.worlds = new ArrayList<>();
        this.name = name;
    }
    
    public List<Player> collectPlayers() {
        List<Player> players = new ArrayList<>();
        for (World world : worlds) {
            for (Player worldPlayers : world.getPlayers()) {
                MetadataValue value = new FixedMetadataValue(AdvancedPlayerList.getInstance(), this);
                worldPlayers.setMetadata("playerGroup:" + name, value);
                worldPlayers.setMetadata("playerGroup", value);
                players.add(worldPlayers);
            }
        }
        return players;
    }
}