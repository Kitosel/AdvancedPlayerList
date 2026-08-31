package pl.kiosel.playerlist.placeholder;

public interface SimplePlaceholder extends Placeholder {
    String replace(String p0, ExtraData p1);

    /**
     * Runs the placeholder once more after AdvancedPlayerList's brace placeholders.
     * This allows mixed expressions such as {@code {format_%player_locale%}} and
     * {@code %player_has_permission_server.view.{world_name}%} to resolve safely.
     */
    default boolean repeatAfterParameterizedPlaceholders() {
        return false;
    }
}
