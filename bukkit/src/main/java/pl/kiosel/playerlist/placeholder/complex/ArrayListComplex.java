package pl.kiosel.playerlist.placeholder.complex;

import java.util.List;

import pl.kiosel.playerlist.placeholder.PlaceholderManager;
import pl.kiosel.playerlist.placeholder.ComplexSession;
import pl.kiosel.playerlist.internal.Slot;
import pl.kiosel.playerlist.placeholder.ExtraData;
import java.util.function.Function;
import java.util.ArrayList;
import pl.kiosel.playerlist.placeholder.ComplexPlaceholder;
import pl.kiosel.rosacore.method.Grouping;

public class ArrayListComplex<T> implements ComplexPlaceholder {
    private final ArrayList<T> array;
    private final Function<T, ExtraData> converter;
    private String overflow;
    private final String name;

    public static <T> Function<T, ExtraData> stringify() {
        return (t -> new Slot().display(String.valueOf(t)));
    }

    public ArrayListComplex(String name, Function<T, ExtraData> converter) {
        this.array = new ArrayList<>();
        this.converter = converter;
        this.name = name;
    }
    
    public ArrayList<T> getList() {
        return this.array;
    }
    
    public void registerPlaceholder() {
        PlaceholderManager.register(this);
    }
    
    public void unregisterPlaceholder() {
        PlaceholderManager.unregister(this);
    }
    
    @Override
    public void onRegistered() {
    }
    
    @Override
    public void onUnregistered() {
    }
    
    public void setOverflowFormat(final String format) {
        this.overflow = format;
    }
    
    @Override
    public String getOverflowFormat() {
        return this.overflow;
    }
    
    @Override
    public String name() {
        return this.name;
    }
    
    @Override
    public ComplexSession newSession(Object viewer, Grouping grouping) {
        List<ExtraData> datas = new ArrayList<>();
        for (int i = this.array.size() - 1; i >= 0; --i) {
            T data = this.array.get(i);
            ExtraData dat = this.converter.apply(data);
            datas.add((dat == null) ? new ExtraData() : dat);
        }
        return new ListSession(datas);
    }
    
    public static class ListSession implements ComplexSession {
        private final ArrayList<ExtraData> array;
        
        public ListSession(List<ExtraData> list) {
            this.array = new ArrayList<>(list);
        }
        
        @Override
        public int getSize() {
            return this.array.size();
        }
        
        @Override
        public ExtraData getValue(final int index) {
            return this.array.get(index);
        }
    }
}
