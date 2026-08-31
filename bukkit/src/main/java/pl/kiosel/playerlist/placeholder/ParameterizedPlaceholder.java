package pl.kiosel.playerlist.placeholder;

public interface ParameterizedPlaceholder extends Placeholder {
    boolean accept(String p0);
    
    String provide(String p0, String p1, ExtraData p2);
}
