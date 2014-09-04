package org.cacert.gigi;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ObjectCache<T extends IdCachable> {

    HashMap<Integer, WeakReference<T>> hashmap = new HashMap<>();

    public void put(T c) {
        hashmap.put(c.getId(), new WeakReference<T>(c));
    }

    public T get(int id) {
        WeakReference<T> res = hashmap.get(id);
        if (res != null) {
            return res.get();
        }
        return null;
    }
}
