package org.cacert.gigi.dbObjects;

import java.util.ArrayList;
import java.util.List;

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

    public void addAdmin(User admin, User actor) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO org_admin SET orgid=?, memid=?, creator=?");
        ps.setInt(1, getId());
        ps.setInt(2, admin.getId());
        ps.setInt(3, actor.getId());
        ps.execute();
    }

    public void removeAdmin(User admin, User actor) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE org_admin SET deleter=?, deleted=NOW() WHERE orgid=? AND memid=?");
        ps.setInt(1, actor.getId());
        ps.setInt(2, getId());
        ps.setInt(3, admin.getId());
        ps.execute();
    }

    public List<User> getAllAdmins() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT memid FROM org_admin WHERE orgid=? AND deleted is null");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        rs.last();
        ArrayList<User> al = new ArrayList<>(rs.getRow());
        rs.beforeFirst();
        while (rs.next()) {
            al.add(User.getById(rs.getInt(1)));
        }
        return al;
    }
}
