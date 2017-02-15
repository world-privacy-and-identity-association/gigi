package club.wpia.gigi.dbObjects;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;

public class ObjectCache<T extends IdCachable> {

    private final HashMap<Integer, WeakReference<T>> hashmap = new HashMap<>();

    private static final HashSet<ObjectCache<? extends IdCachable>> caches = new HashSet<>();

    protected ObjectCache() {
        caches.add(this);
    }

    public T put(T c) {
        hashmap.put(c.getId(), new WeakReference<T>(c));
        return c;
    }

    public T get(int id) {
        WeakReference<T> res = hashmap.get(id);
        if (res != null) {
            return res.get();
        }
        return null;
    }

    public static void clearAllCaches() {
        for (ObjectCache<? extends IdCachable> objectCache : caches) {
            objectCache.hashmap.clear();
        }
    }

    public void remove(T toRm) {
        hashmap.remove(toRm.getId());
    }
}
