package pl.kiosel.playerlist.internal;

import pl.kiosel.playerlist.tablist.Tablist;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class UUIDSet {
    private static final int[] replacement;
    private static UUIDSet set;
    private final UUID[] uuids;
    
    static {
        replacement = new int[] {
                169, 174, 8252, 8265, 8482, 8505, 8596,
                8597, 8598, 8599, 8600, 8601, 8617, 8618,
                8986, 8987, 9000, 9167, 9410, 9642, 9643,
                9654, 9664, 9723, 9724, 9728, 9729, 9730,
                9731, 9732, 9742, 9745, 9748, 9749, 9752,
                9757, 9760, 9762, 9763, 9766, 9770, 9774,
                9775, 9784, 9785, 9786, 9792, 9794, 9800,
                9801, 9802, 9803, 9804, 9805, 9806, 9807,
                9808, 9809, 9810, 9811, 9823, 9824, 9827,
                9829, 9830, 9832, 9851, 9854, 9855, 9874,
                9875, 9876, 9877, 9878, 9879, 9881, 9883,
                9884, 9888, 9889, 9898, 9899, 9904, 9905,
                9986, 9992, 9993, 9996, 9997, 9999, 10002,
                10004, 10006, 10013, 10017, 10035, 10036,
                10052, 10055, 10083, 10084, 10145, 10548,
                10549, 11013, 11014, 11015, 11035, 11036,
                11088, 12336, 12349, 12951, 12953 };
        (UUIDSet.set = new UUIDSet()).initialize();
    }
    
    public static char getPrefix(int index) {
        int replace = UUIDSet.replacement[index % UUIDSet.replacement.length];
        return (char)replace;
    }

    public static void set() {
        (UUIDSet.set = new UUIDSet()).initialize();
    }

    public static UUIDSet getSet() {
        return UUIDSet.set;
    }
    
    public UUIDSet() {
        this.uuids = new UUID[80];
    }
    
    public boolean contains(Tablist tablist, UUID uuid, Player viewer) {
        if (viewer.getUniqueId().equals(uuid) && tablist.isSpectator() && tablist.size() < 80) {
            return true;
        }
        for (UUID uuid1 : uuids)
            if (uuid.equals(uuid1))
                return true;
        return false;
    }
    
    public UUID get(int index) {
        return this.uuids[index];
    }
    
    public UUID getUniqueId(int index, Tablist tablist) {
        int size = tablist.size();
        return (tablist.isSpectator() && size < 80 && tablist.getLastLine() == index) ?
                tablist.getPlayer().getUniqueId() : this.uuids[index];
    }
    
    private void initialize() {
        for (int i = 0; i < this.uuids.length; ++i) {
            this.uuids[i] = UUID.nameUUIDFromBytes(("AdvancedPlayerList:" + i).getBytes(StandardCharsets.UTF_8));
        }
    }
}
