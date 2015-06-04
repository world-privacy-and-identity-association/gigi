package org.cacert.gigi.dbObjects;

import java.util.LinkedList;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public abstract class CertificateOwner implements IdCachable {

    private static ObjectCache<CertificateOwner> myCache = new ObjectCache<>();

    private int id;

    public CertificateOwner(int id) {
        this.id = id;
    }

    public CertificateOwner() {}

    public int getId() {
        return id;
    }

    public static synchronized CertificateOwner getById(int id) {
        CertificateOwner u = myCache.get(id);
        if (u == null) {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT *, users.id AS uid, organisations.id AS oid FROM certOwners LEFT JOIN users ON users.id=certOwners.id LEFT JOIN organisations ON organisations.id = certOwners.id WHERE certOwners.id=? AND deleted is null");
            ps.setInt(1, id);
            try (GigiResultSet rs = ps.executeQuery()) {
                if ( !rs.next()) {
                    return null;
                }
                if (rs.getString("uid") != null) {
                    myCache.put(u = new User(rs));
                } else if (rs.getString("oid") != null) {
                    myCache.put(u = new Organisation(rs));
                } else {
                    System.err.print("Malformed cert owner: " + id);
                }
            }
        }
        return u;
    }

    protected int insert() {
        synchronized (User.class) {
            if (id != 0) {
                throw new Error("refusing to insert");
            }
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO certOwners() VALUES()");
            ps.execute();
            id = ps.lastInsertId();
            myCache.put(this);
        }

        return id;
    }

    public Domain[] getDomains() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM domains WHERE memid=? AND deleted IS NULL");
        ps.setInt(1, getId());

        try (GigiResultSet rs = ps.executeQuery()) {
            LinkedList<Domain> data = new LinkedList<Domain>();

            while (rs.next()) {
                data.add(Domain.getById(rs.getInt(1)));
            }

            return data.toArray(new Domain[0]);
        }
    }

    public Certificate[] getCertificates(boolean includeRevoked) {
        GigiPreparedStatement ps;
        if (includeRevoked) {
            ps = DatabaseConnection.getInstance().prepare("SELECT id FROM certs WHERE memid=?");
        } else {
            ps = DatabaseConnection.getInstance().prepare("SELECT id FROM certs WHERE memid=? AND revoked IS NULL");
        }
        ps.setInt(1, getId());

        try (GigiResultSet rs = ps.executeQuery()) {
            LinkedList<Certificate> data = new LinkedList<Certificate>();

            while (rs.next()) {
                data.add(Certificate.getById(rs.getInt(1)));
            }

            return data.toArray(new Certificate[0]);
        }
    }

    public boolean isValidDomain(String domainname) {
        for (Domain d : getDomains()) {
            String sfx = d.getSuffix();
            if (domainname.equals(sfx) || domainname.endsWith("." + sfx)) {
                return true;
            }
        }

        return false;
    }

    public abstract boolean isValidEmail(String email);

    public void delete() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE certOwners SET deleted=NOW() WHERE id=?");
        ps.setInt(1, getId());
        ps.execute();
        myCache.remove(this);
    }

}
