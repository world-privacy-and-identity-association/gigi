package org.cacert.gigi.dbObjects;

import java.util.ArrayList;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.wrappers.DataContainer;

public class Organisation extends CertificateOwner {

    @DataContainer
    public static class Affiliation {

        private final User target;

        private final boolean master;

        private final String fixedOU;

        private Organisation o;

        public Affiliation(Organisation o, User target, boolean master, String fixedOU) {
            this.o = o;
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
            return o;
        }
    }

    private String name;

    private String state;

    private String province;

    private String city;

    private String email;

    private String optionalName;

    private String postalAddress;

    public Organisation(String name, String state, String province, String city, String email, String optionalName, String postalAddress, User creator) throws GigiApiException {
        if ( !creator.isInGroup(Group.ORGASSURER)) {
            throw new GigiApiException("Only org-assurers may create organisations.");
        }
        this.name = name;
        this.state = state;
        this.province = province;
        this.city = city;
        this.email = email;
        this.optionalName = optionalName;
        this.postalAddress = postalAddress;
        int id = getId();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO organisations SET id=?, name=?, state=?, province=?, city=?, contactEmail=?, optional_name=?, postal_address=?, creator=?")) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, state);
            ps.setString(4, province);
            ps.setString(5, city);
            ps.setString(6, email);
            ps.setString(7, optionalName);
            ps.setString(8, postalAddress);
            ps.setInt(9, creator.getId());
            synchronized (Organisation.class) {
                ps.execute();
            }
        }
    }

    protected Organisation(GigiResultSet rs) {
        super(rs.getInt("id"));
        name = rs.getString("name");
        state = rs.getString("state");
        province = rs.getString("province");
        city = rs.getString("city");
        email = rs.getString("contactEmail");
        optionalName = rs.getString("optional_name");
        postalAddress = rs.getString("postal_address");
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

    public String getContactEmail() {
        return email;
    }

    public String getOptionalName() {
        return optionalName;
    }

    public String getPostalAddress() {
        return postalAddress;
    }

    public static synchronized Organisation getById(int id) {
        CertificateOwner co = CertificateOwner.getById(id);
        if (co instanceof Organisation) {
            return (Organisation) co;
        }
        throw new IllegalArgumentException("Organisation not found.");
    }

    public synchronized void addAdmin(User admin, User actor, boolean master) throws GigiApiException {
        if ( !admin.canAssure()) {
            throw new GigiApiException("Cannot add non-assurer.");
        }
        if ( !actor.isInGroup(Group.ORGASSURER) && !isMaster(actor)) {
            throw new GigiApiException("Only org assurer or master-admin may add admins to an organisation.");
        }
        try (GigiPreparedStatement ps1 = new GigiPreparedStatement("SELECT 1 FROM `org_admin` WHERE `orgid`=? AND `memid`=? AND `deleted` IS NULL")) {
            ps1.setInt(1, getId());
            ps1.setInt(2, admin.getId());
            GigiResultSet result = ps1.executeQuery();
            if (result.next()) {
                return;
            }
        }
        try (GigiPreparedStatement ps2 = new GigiPreparedStatement("INSERT INTO `org_admin` SET `orgid`=?, `memid`=?, `creator`=?, `master`=?::`yesno`")) {
            ps2.setInt(1, getId());
            ps2.setInt(2, admin.getId());
            ps2.setInt(3, actor.getId());
            ps2.setString(4, master ? "y" : "n");
            ps2.execute();
        }
    }

    public void removeAdmin(User admin, User actor) throws GigiApiException {
        if ( !actor.isInGroup(Group.ORGASSURER) && !isMaster(actor)) {
            throw new GigiApiException("Only org assurer or master-admin may delete admins from an organisation.");
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE org_admin SET deleter=?, deleted=NOW() WHERE orgid=? AND memid=?")) {
            ps.setInt(1, actor.getId());
            ps.setInt(2, getId());
            ps.setInt(3, admin.getId());
            ps.execute();
        }
    }

    public List<Affiliation> getAllAdmins() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `memid`, `master` FROM `org_admin` WHERE `orgid`=? AND `deleted` IS NULL", true)) {
            ps.setInt(1, getId());
            GigiResultSet rs = ps.executeQuery();
            rs.last();
            ArrayList<Affiliation> al = new ArrayList<>(rs.getRow());
            rs.beforeFirst();
            while (rs.next()) {
                al.add(new Affiliation(this, User.getById(rs.getInt(1)), rs.getString(2).equals("y"), null));
            }
            return al;
        }
    }

    public static Organisation[] getOrganisations(int offset, int count) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `certOwners`.`id` FROM `organisations` INNER JOIN `certOwners` ON `certOwners`.`id`=`organisations`.`id` WHERE `certOwners`.`deleted` IS NULL OFFSET ? LIMIT ?", true)) {
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
    }

    public void updateCertData(String o, String c, String st, String l) {
        for (Certificate cert : getCertificates(false)) {
            if (cert.getStatus() == CertificateStatus.ISSUED) {
                cert.revoke();
            }
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `organisations` SET `name`=?, `state`=?, `province`=?, `city`=? WHERE `id`=?")) {
            ps.setString(1, o);
            ps.setString(2, c);
            ps.setString(3, st);
            ps.setString(4, l);
            ps.setInt(5, getId());
            ps.executeUpdate();
        }
        name = o;
        state = c;
        province = st;
        city = l;
    }

    public void updateOrgData(String mail, String o_name, String p_address) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `organisations` SET `contactEmail`=?, `optional_name`=?, `postal_address`=? WHERE `id`=?")) {
            ps.setString(1, mail);
            ps.setString(2, o_name);
            ps.setString(3, p_address);
            ps.setInt(4, getId());
            ps.executeUpdate();
        }
        email = mail;
        optionalName = o_name;
        postalAddress = p_address;
    }

    public boolean isMaster(User u) {
        for (Affiliation i : getAllAdmins()) {
            if (i.isMaster() && i.getTarget() == u) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidEmail(String email) {
        return isValidDomain(email.split("@", 2)[1]);
    }

    public static final String SELF_ORG_NAME = "SomeCA";

    public boolean isSelfOrganisation() {
        return SELF_ORG_NAME.equals(getName());
    }
}
