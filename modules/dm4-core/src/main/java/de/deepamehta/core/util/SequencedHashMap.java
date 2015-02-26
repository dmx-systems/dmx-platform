package de.deepamehta.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;



public class SequencedHashMap<K,V> extends LinkedHashMap<K,V> {

    /**
     * @param   beforeKey   the key <i>before</i> the key-value entry is put.
     *                      If <code>null</code> the entry is put at the end.
     *                      If non-<code>null</code> but not contained in the map an exception is thrown.
     */
    public void putBefore(K key, V value, K beforeKey) {
        // collect keys of entries to shift
        List<K> shiftKeys = new ArrayList<K>();
        if (beforeKey != null) {
            boolean shift = false;
            for (K k : keySet()) {
                if (!shift && k.equals(beforeKey)) {
                    shift = true;
                }
                if (shift) {
                    shiftKeys.add(k);
                }
            }
            //
            if (shiftKeys.isEmpty()) {
                throw new RuntimeException("Key \"" + beforeKey + "\" not found in " + keySet());
            }
        }
        //
        put(key, value);
        //
        // shift entries
        for (K k : shiftKeys) {
            put(k, remove(k));
        }
    }
}
