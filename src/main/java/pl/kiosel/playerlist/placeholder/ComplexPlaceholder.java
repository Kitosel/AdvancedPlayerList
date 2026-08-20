package pl.kiosel.playerlist.placeholder;

import pl.kiosel.rosacore.method.Grouping;

public interface ComplexPlaceholder extends Placeholder {
    String getOverflowFormat();
    String name();
    ComplexSession newSession(Object p0, Grouping p1);
}
