package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;

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

    // Cria a entrada se não existir; incrementa o contador caso exista
    public void putOrIncrement(K key) {
        map.compute(key, (k, entry) -> {
            if (entry == null) {
                entry = new EntryData();
            }
            entry.incrementCounter();
            return entry;
        });
    }

    // Remove diretamente uma entrada
    public void remove(K key) {
        map.remove(key);
    }

    // Acede à entrada associada à chave
    public EntryData getEntry(K key) {
        return map.get(key);
    }

    // Verifica se a chave existe
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public Set<K> getAllKeys() {
        return map.keySet();
    }
}

