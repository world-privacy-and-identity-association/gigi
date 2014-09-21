package org.cacert.gigi.dbObjects;

import java.util.HashMap;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

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

    public User[] getMembers(int offset, int count) {
        GigiPreparedStatement gps = DatabaseConnection.getInstance().prepare("SELECT user FROM user_groups WHERE permission=? AND deleted is NULL LIMIT ?,?");
        gps.setString(1, dbName);
        gps.setInt(2, offset);
        gps.setInt(3, count);
        GigiResultSet grs = gps.executeQuery();
        grs.last();
        User[] users = new User[grs.getRow()];
        int i = 0;
        grs.beforeFirst();
        while (grs.next()) {
            users[i++] = User.getById(grs.getInt(1));
        }
        return users;
    }
}
