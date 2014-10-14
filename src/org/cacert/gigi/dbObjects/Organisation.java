package org.cacert.gigi.dbObjects;

import java.util.ArrayList;
import java.util.List;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;

public class Organisation extends CertificateOwner {

    public class Affiliation {

        private final User target;

        private final boolean master;

        private final String fixedOU;

        public Affiliation(User target, boolean master, String fixedOU) {
            this.target = target;
            this.master = master;
            this.fixedOU = fixedOU;
        }

        public User getTarget() {
            return target;
        }

        public boolean isMaster() {
            return master;
        }

        public String getFixedOU() {
            return fixedOU;
        }

        public Organisation getOrganisation() {
            return Organisation.this;
        }
    }

    private String name;

    private String state;

    private String province;

    private String city;

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
        super(rs.getInt("id"));
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

    public void addAdmin(User admin, User actor, boolean master) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO org_admin SET orgid=?, memid=?, creator=?, master=?");
        ps.setInt(1, getId());
        ps.setInt(2, admin.getId());
        ps.setInt(3, actor.getId());
        ps.setString(4, master ? "y" : "n");
        ps.execute();
    }

    public void removeAdmin(User admin, User actor) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE org_admin SET deleter=?, deleted=NOW() WHERE orgid=? AND memid=?");
        ps.setInt(1, actor.getId());
        ps.setInt(2, getId());
        ps.setInt(3, admin.getId());
        ps.execute();
    }

    public List<Affiliation> getAllAdmins() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT memid, master FROM org_admin WHERE orgid=? AND deleted is null");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        rs.last();
        ArrayList<Affiliation> al = new ArrayList<>(rs.getRow());
        rs.beforeFirst();
        while (rs.next()) {
            al.add(new Affiliation(User.getById(rs.getInt(1)), rs.getString(2).equals("y"), null));
        }
        return al;
    }

    public static Organisation[] getOrganisations(int offset, int count) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM organisations LIMIT ?,?");
        ps.setInt(1, offset);
        ps.setInt(2, count);
        GigiResultSet res = ps.executeQuery();
        res.last();
        Organisation[] resu = new Organisation[res.getRow()];
        res.beforeFirst();
        int i = 0;
        while (res.next()) {
            resu[i++] = getById(res.getInt(1));
        }
        return resu;
    }

    public void update(String o, String c, String st, String l) {
        for (Certificate cert : getCertificates()) {
            if (cert.getStatus() == CertificateStatus.ISSUED) {
                cert.revoke();
            }
        }
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE organisations SET name=?, state=?, province=?, city=?");
        ps.setString(1, o);
        ps.setString(2, c);
        ps.setString(3, st);
        ps.setString(4, l);
        ps.execute();
        name = o;
        state = c;
        province = st;
        city = l;
    }
}
