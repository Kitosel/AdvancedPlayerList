package pl.kiosel.playerlist.internal;

import java.math.BigDecimal;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ExtraData;

import java.util.List;

@SuppressWarnings("unchecked")
public enum SortOrder {

    ASCEND(0, 1),
    DESCEND(1, -1);
    
    private final int order;
    private final int ordinal;
    
    public static SortOrder order(String order) {
        if ("DESCEND".equals(order)) {
            return SortOrder.DESCEND;
        }
        return SortOrder.ASCEND;
    }
    
    public SortOrder revert() {
        return (this == SortOrder.ASCEND) ? SortOrder.DESCEND : SortOrder.ASCEND;
    }
    
    SortOrder(int ordinal, int order) {
        this.ordinal = ordinal;
        this.order = order;
    }

    public <T> List<T> sort(Object viewer, String sort, String placeholderKey, List<T> list) {
        String[] split = (sort == null) ? new String[0] : sort.split(";");
        if (sort != null)
            list.sort((a, b) -> {
                ExtraData dataA = (new ExtraData())
                        .put(ExtraData.DATA_PLAYER, viewer)
                        .put(ExtraData.DATA_VIEWER, viewer)
                        .put(placeholderKey, a);
                ExtraData dataB = (new ExtraData())
                        .put(ExtraData.DATA_PLAYER, viewer)
                        .put(ExtraData.DATA_VIEWER, viewer)
                        .put(placeholderKey, b);

                for (String part : split) {
                    SortOrder order = this;
                    if (part.startsWith("!")) {
                        part = part.substring(1);
                        order = revert();
                    }
                    String partA = PlaceholderManager.replace(part, dataA);
                    String partB = PlaceholderManager.replace(part, dataB);
                    int result = compare(partA, partB) * order.order;
                    if (result != 0)
                        return result;
                }
                return (a instanceof Comparable && b instanceof Comparable) ?
                        (((Comparable<T>)a).compareTo(b) * this.order) : 0;
            });
        return list;
    }
    
    static int compare(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (Throwable t) {
            try {
                return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
            } catch (Throwable t2) {
                try {
                    return new BigDecimal(a).compareTo(new BigDecimal(b));
                } catch (Throwable t3) {
                    return a.compareTo(b);
                }
            }
        }
    }
}
