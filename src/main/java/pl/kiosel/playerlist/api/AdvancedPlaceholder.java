package pl.kiosel.playerlist.api;

/**
 * Public placeholder contract for integrations with AdvancedPlayerList.
 *
 * <p>An identifier {@code village} handles both {@code {village}} and
 * {@code {village_name}}. In the second form {@code name} is passed as the parameter.</p>
 */
public interface AdvancedPlaceholder {

    String getIdentifier();

    String onRequest(PlaceholderContext context, String parameter);

    default void onRegistered() {
    }

    default void onUnregistered() {
    }
}
