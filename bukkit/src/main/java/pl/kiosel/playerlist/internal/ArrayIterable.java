package pl.kiosel.playerlist.internal;

import java.util.Arrays;

import pl.kiosel.playerlist.model.Ticker;

public class ArrayIterable<T> {

    private int index;
    private final T[] array;
    private final int[] delay;
    
    public ArrayIterable(T[] array, int[] delay) {
        this.index = 0;
        this.array = array;
        this.delay = delay;
    }
    
    public T getCurrent() {
        return (this.array == null || this.array.length == 0) ? null : this.array[this.index];
    }
    
    public int length() {
        return (this.array == null) ? 0 : this.array.length;
    }
    
    public void next() {
        if (this.array == null || this.array.length <= 1) {
            return;
        }
        this.index = (this.index + 1) % this.array.length;
    }
    
    public void tick() {
        if (this.array != null && this.array.length > 1) {
            int current = this.delay[this.index];
            if (current <= 0) {
                current = 1;
            }
            if (Ticker.tickTime() % current == 0) {
                this.next();
            }
        }
    }
    
    @Override
    public String toString() {
        return Arrays.toString(this.array);
    }
}
