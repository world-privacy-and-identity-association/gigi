package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public abstract class CertificateOwner implements IdCachable {

    private int id;

    public CertificateOwner(int id) {
        this.id = id;
    }

    public CertificateOwner() {}

    public int getId() {
        return id;
    }

    private static ObjectCache<CertificateOwner> myCache = new ObjectCache<>();

    public static synchronized CertificateOwner getById(int id) {
        CertificateOwner u = myCache.get(id);
        if (u == null) {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT *, users.id AS uid, organisations.id AS oid FROM certOwners LEFT JOIN users ON users.id=certOwners.id LEFT JOIN organisations ON organisations.id = certOwners.id WHERE certOwners.id=?");
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                System.out.println("no " + id);
            }
            if (rs.getString("uid") != null) {
                myCache.put(u = new User(rs));
            } else if (rs.getString("oid") != null) {
                myCache.put(u = new Organisation(rs));
            } else {
                System.err.print("Malformed cert owner: " + id);
            }
        }
        return u;
    }

    protected int insert() {
        if (id != 0) {
            throw new Error("refusing to insert");
        }
        synchronized (User.class) {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO certOwners() VALUES()");
            ps.execute();
            id = ps.lastInsertId();
            myCache.put(this);
        }
        return id;
    }

}
