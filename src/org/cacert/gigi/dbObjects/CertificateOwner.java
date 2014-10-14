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

    public EmailAddress[] getEmails() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM emails WHERE memid=? AND deleted is NULL");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        rs.last();
        int count = rs.getRow();
        EmailAddress[] data = new EmailAddress[count];
        rs.beforeFirst();
        for (int i = 0; i < data.length; i++) {
            if ( !rs.next()) {
                throw new Error("Internal sql api violation.");
            }
            data[i] = EmailAddress.getById(rs.getInt(1));
        }
        rs.close();
        return data;

    }

    public Domain[] getDomains() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM domains WHERE memid=? AND deleted IS NULL");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        rs.last();
        int count = rs.getRow();
        Domain[] data = new Domain[count];
        rs.beforeFirst();
        for (int i = 0; i < data.length; i++) {
            if ( !rs.next()) {
                throw new Error("Internal sql api violation.");
            }
            data[i] = Domain.getById(rs.getInt(1));
        }
        rs.close();
        return data;

    }

    public Certificate[] getCertificates() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT serial FROM certs WHERE memid=? AND revoked IS NULL");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        rs.last();
        int count = rs.getRow();
        Certificate[] data = new Certificate[count];
        rs.beforeFirst();
        for (int i = 0; i < data.length; i++) {
            if ( !rs.next()) {
                throw new Error("Internal sql api violation.");
            }
            data[i] = Certificate.getBySerial(rs.getString(1));
        }
        rs.close();
        return data;

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

    public boolean isValidEmail(String email) {
        for (EmailAddress em : getEmails()) {
            if (em.getAddress().equals(email)) {
                return true;
            }
        }
        return false;
    }

}
