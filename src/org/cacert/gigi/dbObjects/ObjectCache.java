package org.cacert.gigi.dbObjects;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;

public class ObjectCache<T extends IdCachable> {

    private HashMap<Integer, WeakReference<T>> hashmap = new HashMap<>();

    private static HashSet<ObjectCache<?>> caches = new HashSet<>();

    protected ObjectCache() {
        caches.add(this);
    }

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

    public static void clearAllCaches() {
        for (ObjectCache<?> objectCache : caches) {
            objectCache.hashmap.clear();
        }
    }

    public void remove(T toRm) {
        hashmap.remove(toRm);
    }
}
