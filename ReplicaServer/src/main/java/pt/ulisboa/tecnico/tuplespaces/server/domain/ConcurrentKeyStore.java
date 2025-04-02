package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;

/*
    Class representing a key map (tuples) with associated counters
    Each key has an associated counter that is incremented and decremented
    The counter is incremented when a tuple is inserted and decremented when it is removed
    The key is removed from the map when the counter reaches 0
    The class includes per-key lock implementation to ensure counter consistency
*/

public class ConcurrentKeyStore<K> {

    public static class EntryData {
        final ReentrantLock lock = new ReentrantLock();
        private int counter = 0;

        public void incrementCounter() {
            counter++;
        }

        public void decrementCounter() {
            counter--;
        }

        public int getCounter() {
            return counter;
        }
    }

    private final ConcurrentHashMap<K, EntryData> map = new ConcurrentHashMap<>();

    // Creates the key if it does not exist and increments the respective counter
    public void putOrIncrement(K key) {
        map.compute(key, (k, entry) -> {
            if (entry == null) {
                entry = new EntryData();
            }
            entry.incrementCounter();
            return entry;
        });
    }

    // Removes the key
    public void remove(K key) {
        map.remove(key);
    }

    // Access the respective key
    public EntryData getEntry(K key) {
        return map.get(key);
    }

    // Returns all keys
    public Set<K> getAllKeys() {
        return map.keySet();
    }
}

