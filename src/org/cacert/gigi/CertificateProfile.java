package org.cacert.gigi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.cacert.gigi.database.DatabaseConnection;

public class CertificateProfile {

    final int id;

    final String keyName;

    final String visibleName;

    static HashMap<String, CertificateProfile> byName = new HashMap<>();

    static HashMap<Integer, CertificateProfile> byId = new HashMap<>();

    private CertificateProfile(int id, String keyName, String visibleName) {
        this.id = id;
        this.keyName = keyName;
        this.visibleName = visibleName;
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

    static {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id, keyname, name FROM `profiles`");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CertificateProfile cp = new CertificateProfile(rs.getInt("id"), rs.getString("keyName"), rs.getString("name"));
                byId.put(cp.getId(), cp);
                byName.put(cp.getKeyName(), cp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static CertificateProfile getById(int id) {
        return byId.get(id);
    }

    public static CertificateProfile getByName(String name) {
        return byName.get(name);
    }

}
