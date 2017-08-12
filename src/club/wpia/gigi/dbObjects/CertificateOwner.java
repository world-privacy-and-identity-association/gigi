package club.wpia.gigi.dbObjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;

public abstract class CertificateOwner implements IdCachable, Serializable {

    private static final long serialVersionUID = -672580485730247314L;

    private static final ObjectCache<CertificateOwner> myCache = new ObjectCache<>();

    private int id;

    protected CertificateOwner(int id) {
        this.id = id;
    }

    protected CertificateOwner() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `certOwners` DEFAULT VALUES")) {
            ps.execute();
            id = ps.lastInsertId();
        }
        myCache.put(this);
    }

    public int getId() {
        return id;
    }

    public static synchronized CertificateOwner getById(int id) {
        CertificateOwner cached = myCache.get(id);
        if (cached != null) {
            return cached;
        }

        try (GigiPreparedStatement psU = new GigiPreparedStatement("SELECT *, `users`.`id` AS uid FROM `certOwners` INNER JOIN `users` ON `users`.`id`=`certOwners`.`id` WHERE `certOwners`.`id`=? AND `deleted` is null")) {
            psU.setInt(1, id);
            GigiResultSet rsU = psU.executeQuery();
            if (rsU.next()) {
                return myCache.put(new User(rsU));
            }
        } catch (GigiApiException e) {
            throw new Error(e);
        }

        try (GigiPreparedStatement psO = new GigiPreparedStatement("SELECT *, `organisations`.`id` AS oid FROM `certOwners` INNER JOIN `organisations` ON `organisations`.`id`=`certOwners`.`id` WHERE `certOwners`.`id`=? AND `deleted` is null")) {
            psO.setInt(1, id);
            GigiResultSet rsO = psO.executeQuery();
            if (rsO.next()) {
                return myCache.put(new Organisation(rsO));
            }
        } catch (GigiApiException e) {
            throw new Error(e);
        }

        System.err.println("Malformed cert owner: " + id);
        return null;
    }

    public Domain[] getDomains() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `domains` WHERE `memid`=? AND `deleted` IS NULL")) {
            ps.setInt(1, getId());

            try (GigiResultSet rs = ps.executeQuery()) {
                LinkedList<Domain> data = new LinkedList<Domain>();

                while (rs.next()) {
                    data.add(Domain.getById(rs.getInt(1)));
                }

                return data.toArray(new Domain[0]);
            }
        }
    }

    public Certificate[] getCertificates(boolean includeRevoked) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement(includeRevoked ? "SELECT id FROM certs WHERE memid=? ORDER BY id DESC" : "SELECT id FROM certs WHERE memid=? AND `revoked` IS NULL ORDER BY id DESC")) {
            ps.setInt(1, getId());

            GigiResultSet rs = ps.executeQuery();
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
                return d.isVerified();
            }
        }

        return false;
    }

    public abstract boolean isValidEmail(String email);

    public void delete() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `certOwners` SET `deleted`=NOW() WHERE `id`=?")) {
            ps.setInt(1, getId());
            ps.execute();
        }
        myCache.remove(this);
    }

    public String[] getAdminLog() {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT `when`, type, information FROM `adminLog` WHERE uid=? ORDER BY `when` ASC")) {
            prep.setInt(1, getId());
            GigiResultSet res = prep.executeQuery();
            List<String> entries = new LinkedList<String>();

            while (res.next()) {
                entries.add(res.getString(2) + " (" + res.getString(3) + ")");
            }
            return entries.toArray(new String[0]);
        }
    }

    public static CertificateOwner getByEnabledSerial(String serial) {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT `memid` FROM `certs` INNER JOIN `logincerts` ON `logincerts`.`id`=`certs`.`id` WHERE serial=? AND `revoked` is NULL")) {
            prep.setString(1, serial);
            GigiResultSet res = prep.executeQuery();
            if (res.next()) {
                return getById(res.getInt(1));
            }
            return null;
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeLong(getId());
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        id = (int) ois.readLong();
    }

    protected Object readResolve() throws ObjectStreamException {
        /**
         * Returning the Object by looking up its ID in the cache.
         *
         * @see http://www.javalobby.org/java/forums/t17491.html
         * @see http://www.jguru.com/faq/view.jsp?EID=44039
         * @see http://thecodersbreakfast.net/
         *      ?post/2011/05/12/Serialization-and-magic-methods
         */
        CertificateOwner co = getById(this.getId());

        if (null == co) {
            throw new Error("Unknown Certificate Owner");
        }

        return co;
    }

}
