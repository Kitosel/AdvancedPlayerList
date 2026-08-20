package pl.kiosel.playerlist.internal;

import pl.kiosel.playerlist.model.Ticker;

public interface Tickable extends Runnable {

    default void registerTask() {
        Ticker.register(this);
    }
    
    default int tickTime() {
        return Ticker.tickTime();
    }
    
    default void unregisterTask() {
        Ticker.unregister(this);
    }
    
    default boolean optimize() {
        return Ticker.optimize();
    }
    
    default void setTimeout(Runnable r, int millis) {
        Ticker.delayMillis(r, millis);
    }
}
