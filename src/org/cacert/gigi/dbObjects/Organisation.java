package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class Organisation extends CertificateOwner {

    private final String name;

    private final String state;

    private final String province;

    private final String city;

    public Organisation(String name, String state, String province, String city, User creator) {
        this.name = name;
        this.state = state;
        this.province = province;
        this.city = city;
        int id = super.insert();
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO organisations SET id=?, name=?, state=?, province=?, city=?, creator=?");
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setString(3, state);
        ps.setString(4, province);
        ps.setString(5, city);
        ps.setInt(6, creator.getId());
        synchronized (Organisation.class) {
            ps.execute();
        }

    }

    protected Organisation(GigiResultSet rs) {
        name = rs.getString("name");
        state = rs.getString("state");
        province = rs.getString("province");
        city = rs.getString("city");
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public static synchronized Organisation getById(int id) {
        CertificateOwner co = CertificateOwner.getById(id);
        if (co instanceof Organisation) {
            return (Organisation) co;
        }
        return null;
    }
}
