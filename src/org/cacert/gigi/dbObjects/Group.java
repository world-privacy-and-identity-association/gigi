package org.cacert.gigi.dbObjects;

import java.util.HashMap;

public class Group {

    private static HashMap<String, Group> cache = new HashMap<>();

    private final String dbName;

    private Group(String name) {
        dbName = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dbName == null) ? 0 : dbName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Group other = (Group) obj;
        if (dbName == null) {
            if (other.dbName != null) {
                return false;
            }
        } else if ( !dbName.equals(other.dbName)) {
            return false;
        }
        return true;
    }

    public static synchronized Group getByString(String name) {
        Group g = cache.get(name);
        if (g == null) {
            g = new Group(name);
            cache.put(name, g);
        }
        return g;
    }

    public String getDatabaseName() {
        return dbName;
    }
}
