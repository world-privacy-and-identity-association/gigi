package org.cacert.gigi.dbObjects;

import java.util.HashMap;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class CertificateProfile {

    private final int id;

    private final String keyName;

    private final String visibleName;

    private final int caId;

    private static HashMap<String, CertificateProfile> byName = new HashMap<>();

    private static HashMap<Integer, CertificateProfile> byId = new HashMap<>();

    private CertificateProfile(int id, String keyName, String visibleName, int caId) {
        this.id = id;
        this.keyName = keyName;
        this.visibleName = visibleName;
        this.caId = caId;
    }

    public int getId() {
        return id;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public int getCAId() {
        return caId;
    }

    static {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id, keyname, name, rootcert FROM `profiles`");
        GigiResultSet rs = ps.executeQuery();
        while (rs.next()) {
            CertificateProfile cp = new CertificateProfile(rs.getInt("id"), rs.getString("keyName"), rs.getString("name"), rs.getInt("rootcert"));
            byId.put(cp.getId(), cp);
            byName.put(cp.getKeyName(), cp);
        }

    }

    public static CertificateProfile getById(int id) {
        return byId.get(id);
    }

    public static CertificateProfile getByName(String name) {
        return byName.get(name);
    }

    public static CertificateProfile[] getAll() {
        return byId.values().toArray(new CertificateProfile[byId.size()]);
    }

}
