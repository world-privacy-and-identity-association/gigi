package org.cacert.gigi.util;

import java.util.HashMap;
import java.util.TreeSet;

import org.cacert.gigi.GigiApiException;

public class RateLimit {

    public static final class RateLimitException extends GigiApiException {

        private static final long serialVersionUID = 1L;

        public RateLimitException() {
            super("Rate limit exceeded.");
        }
    }

    private class Entry implements Comparable<Entry> {

        long firstAccess;

        int count = 1;

        String feature;

        public Entry(long firstAccess, String feature) {
            this.firstAccess = firstAccess;
            this.feature = feature;
        }

        public void access() {
            count++;
        }

        @Override
        public int compareTo(Entry o) {
            return feature.compareTo(o.feature);
        }

        public boolean isExpired() {
            return firstAccess + time < System.currentTimeMillis();
        }

    }

    private final int maxcount;

    private final long time;

    TreeSet<Entry> set = new TreeSet<Entry>();

    HashMap<String, Entry> feat = new HashMap<>();

    public RateLimit(int maxcount, long time) {
        this.maxcount = maxcount;
        this.time = time;
    }

    public synchronized boolean isLimitExceeded(String feature) {
        clean();
        Entry e = feat.get(feature);
        if (e == null) {
            e = new Entry(System.currentTimeMillis(), feature);
            set.add(e);
            feat.put(feature, e);
        } else {
            e.access();
        }
        return e.count > maxcount;
    }

    private void clean() {
        while (set.size() > 0) {
            Entry e = set.last();
            if (e.isExpired()) {
                set.remove(e);
                feat.remove(e.feature);
            } else {
                return;
            }
        }
    }

    public synchronized void bypass() {
        set.clear();
        feat.clear();
    }
}
