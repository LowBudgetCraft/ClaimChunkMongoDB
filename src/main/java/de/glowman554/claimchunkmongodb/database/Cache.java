package de.glowman554.claimchunkmongodb.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public abstract class Cache<K, V> {
    private final HashMap<K, V> cache = new HashMap<>();
    private boolean completeData = false;

    public V get(K key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        V value = queryDB(key);
        cache.put(key, value);
        return value;
    }

    public Collection<V> all() {
        if (completeData) {
            return cache.values().stream().filter(Objects::nonNull).toList();
        }
        List<V> all = queryDBAll();
        for (V value : all) {
            cache.put(extractKey(value), value);
        }
        completeData = true;
        return all;
    }

    public void uncache(K key) {
        cache.remove(key);
        completeData = false;
    }

    public void cache(K key, V value) {
        cache.put(key, value);
    }

    public void delete(K key) {
        cache.remove(key);
        deleteDB(key);
    }

    public void insert(V[] values) {
        insertDB(values);
        for (V value : values) {
            cache.put(extractKey(value), value);
        }
    }

    public void clear() {
        cache.clear();
        completeData = false;
    }


    protected abstract V queryDB(K key);

    protected abstract void deleteDB(K key);

    protected abstract List<V> queryDBAll();

    protected abstract K extractKey(V value);

    protected abstract void insertDB(V[] values);

    public HashMap<K, V> getCache() {
        return cache;
    }
}
