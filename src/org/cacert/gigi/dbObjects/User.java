package org.cacert.gigi.dbObjects;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PasswordHash;
import org.cacert.gigi.util.PasswordStrengthChecker;

public class User extends CertificateOwner {

    private Name name = new Name(null, null, null, null);

    private Date dob;

    private String email;

    private Assurance[] receivedAssurances, madeAssurances;

    private Locale locale;

    private Set<Group> groups = new HashSet<>();

    protected User(GigiResultSet rs) {
        super(rs.getInt("id"));
        updateName(rs);
    }

    private void updateName(GigiResultSet rs) {
        name = new Name(rs.getString("fname"), rs.getString("lname"), rs.getString("mname"), rs.getString("suffix"));
        dob = rs.getDate("dob");
        email = rs.getString("email");
        String localeStr = rs.getString("language");
        if (localeStr == null || localeStr.equals("")) {
            locale = Locale.getDefault();
        } else {
            locale = Language.getLocaleFromString(localeStr);
        }
        GigiPreparedStatement psg = DatabaseConnection.getInstance().prepare("SELECT permission FROM user_groups WHERE user=? AND deleted is NULL");
        psg.setInt(1, rs.getInt("id"));
        GigiResultSet rs2 = psg.executeQuery();
        while (rs2.next()) {
            groups.add(Group.getByString(rs2.getString(1)));
        }
        rs2.close();
    }

    public User() {}

    public String getFname() {
        return name.fname;
    }

    public String getLname() {
        return name.lname;
    }

    public String getMname() {
        return name.mname;
    }

    public Name getName() {
        return name;
    }

    public void setMname(String mname) {
        this.name.mname = mname;
    }

    public String getSuffix() {
        return name.suffix;
    }

    public void setSuffix(String suffix) {
        this.name.suffix = suffix;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFname(String fname) {
        this.name.fname = fname;
    }

    public void setLname(String lname) {
        this.name.lname = lname;
    }

    public void insert(String password) {
        int id = super.insert();
        GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("insert into `users` set `email`=?, `password`=?, " + "`fname`=?, `mname`=?, `lname`=?, " + "`suffix`=?, `dob`=?, `language`=?, id=?");
        query.setString(1, email);
        query.setString(2, PasswordHash.hash(password));
        query.setString(3, name.fname);
        query.setString(4, name.mname);
        query.setString(5, name.lname);
        query.setString(6, name.suffix);
        query.setDate(7, new java.sql.Date(dob.getTime()));
        query.setString(8, locale.toString());
        query.setInt(9, id);
        query.execute();
    }

    public void changePassword(String oldPass, String newPass) throws GigiApiException {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `password` FROM users WHERE id=?");
        ps.setInt(1, getId());
        GigiResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            throw new GigiApiException("User not found... very bad.");
        }
        if (PasswordHash.verifyHash(oldPass, rs.getString(1)) == null) {
            throw new GigiApiException("Old password does not match.");
        }
        rs.close();
        PasswordStrengthChecker.assertStrongPassword(newPass, this);
        ps = DatabaseConnection.getInstance().prepare("UPDATE users SET `password`=? WHERE id=?");
        ps.setString(1, PasswordHash.hash(newPass));
        ps.setInt(2, getId());
        if (ps.executeUpdate() != 1) {
            throw new GigiApiException("Password update failed.");
        }
    }

    public void setName(Name name) {
        this.name = name;
    }

    public boolean canAssure() {
        if ( !isOfAge(14)) { // PoJAM
            return false;
        }
        if (getAssurancePoints() < 100) {
            return false;
        }

        return hasPassedCATS();

    }

    public boolean hasPassedCATS() {
        GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `cats_passed` where `user_id`=?");
        query.setInt(1, getId());
        GigiResultSet rs = query.executeQuery();
        if (rs.next()) {
            return true;
        } else {
            return false;
        }
    }

    public int getAssurancePoints() {
        GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT sum(points) FROM `notary` where `to`=? AND `deleted` is NULL");
        query.setInt(1, getId());
        GigiResultSet rs = query.executeQuery();
        int points = 0;
        if (rs.next()) {
            points = rs.getInt(1);
        }
        rs.close();
        return points;
    }

    public int getExperiencePoints() {
        GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT count(*) FROM `notary` where `from`=? AND `deleted` is NULL");
        query.setInt(1, getId());
        GigiResultSet rs = query.executeQuery();
        int points = 0;
        if (rs.next()) {
            points = rs.getInt(1) * 2;
        }
        rs.close();
        return points;
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof User)) {
            return false;
        }
        User s = (User) obj;
        return name.equals(s.name) && email.equals(s.email) && dob.toString().equals(s.dob.toString()); // This
                                                                                                        // is
                                                                                                        // due
                                                                                                        // to
                                                                                                        // day
                                                                                                        // cutoff
    }

    /**
     * Gets the maximum allowed points NOW. Note that an assurance needs to
     * re-check PoJam as it has taken place in the past.
     * 
     * @return the maximal points @
     */
    public int getMaxAssurePoints() {
        if ( !isOfAge(18)) {
            return 10; // PoJAM
        }

        int exp = getExperiencePoints();
        int points = 10;

        if (exp >= 10) {
            points += 5;
        }
        if (exp >= 20) {
            points += 5;
        }
        if (exp >= 30) {
            points += 5;
        }
        if (exp >= 40) {
            points += 5;
        }
        if (exp >= 50) {
            points += 5;
        }
        return points;
    }

    public boolean isOfAge(int desiredAge) {
        Calendar c = Calendar.getInstance();
        c.setTime(dob);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        c.set(year, month, day);
        c.add(Calendar.YEAR, desiredAge);
        return System.currentTimeMillis() >= c.getTime().getTime();
    }

    public boolean isValidName(String name) {
        return getName().matches(name);
    }

    public void updateDefaultEmail(EmailAddress newMail) throws GigiApiException {
        EmailAddress[] adrs = getEmails();
        for (int i = 0; i < adrs.length; i++) {
            if (adrs[i].getAddress().equals(newMail.getAddress())) {
                if ( !adrs[i].isVerified()) {
                    throw new GigiApiException("Email not verified.");
                }
                GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE users SET email=? WHERE id=?");
                ps.setString(1, newMail.getAddress());
                ps.setInt(2, getId());
                ps.execute();
                email = newMail.getAddress();
                return;
            }
        }
        throw new GigiApiException("Given address not an address of the user.");
    }

    public void deleteEmail(EmailAddress mail) throws GigiApiException {
        if (getEmail().equals(mail.getAddress())) {
            throw new GigiApiException("Can't delete user's default e-mail.");
        }
        EmailAddress[] emails = getEmails();
        for (int i = 0; i < emails.length; i++) {
            if (emails[i].getId() == mail.getId()) {
                GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE emails SET deleted=? WHERE id=?");
                ps.setDate(1, new Date(System.currentTimeMillis()));
                ps.setInt(2, mail.getId());
                ps.execute();
                return;
            }
        }
        throw new GigiApiException("Email not one of user's email addresses.");
    }

    public Assurance[] getReceivedAssurances() {
        if (receivedAssurances == null) {
            GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT * FROM notary WHERE `to`=? AND deleted IS NULL");
            query.setInt(1, getId());
            GigiResultSet res = query.executeQuery();
            res.last();
            Assurance[] assurances = new Assurance[res.getRow()];
            res.beforeFirst();
            for (int i = 0; i < assurances.length; i++) {
                res.next();
                assurances[i] = new Assurance(res);
            }
            this.receivedAssurances = assurances;
            return assurances;
        }
        return receivedAssurances;
    }

    public Assurance[] getMadeAssurances() {
        if (madeAssurances == null) {
            GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT * FROM notary WHERE `from`=? AND deleted is NULL");
            query.setInt(1, getId());
            GigiResultSet res = query.executeQuery();
            res.last();
            Assurance[] assurances = new Assurance[res.getRow()];
            res.beforeFirst();
            for (int i = 0; i < assurances.length; i++) {
                res.next();
                assurances[i] = new Assurance(res);
            }
            this.madeAssurances = assurances;
            return assurances;
        }
        return madeAssurances;
    }

    public void invalidateMadeAssurances() {
        madeAssurances = null;
    }

    public void invalidateReceivedAssurances() {
        receivedAssurances = null;
    }

    public void updateUserData() throws GigiApiException {
        synchronized (Notary.class) {
            if (getAssurancePoints() != 0) {
                throw new GigiApiException("No change after assurance allowed.");
            }
            GigiPreparedStatement update = DatabaseConnection.getInstance().prepare("UPDATE users SET fname=?, lname=?, mname=?, suffix=?, dob=? WHERE id=?");
            update.setString(1, getFname());
            update.setString(2, getLname());
            update.setString(3, getMname());
            update.setString(4, getSuffix());
            update.setDate(5, getDob());
            update.setInt(6, getId());
            update.executeUpdate();
        }
    }

    public Locale getPreferredLocale() {
        return locale;
    }

    public void setPreferredLocale(Locale locale) {
        this.locale = locale;

    }

    public boolean wantsDirectoryListing() {
        GigiPreparedStatement get = DatabaseConnection.getInstance().prepare("SELECT listme FROM users WHERE id=?");
        get.setInt(1, getId());
        GigiResultSet exec = get.executeQuery();
        exec.next();
        return exec.getBoolean("listme");
    }

    public String getContactInformation() {
        GigiPreparedStatement get = DatabaseConnection.getInstance().prepare("SELECT contactinfo FROM users WHERE id=?");
        get.setInt(1, getId());
        GigiResultSet exec = get.executeQuery();
        exec.next();
        return exec.getString("contactinfo");
    }

    public void setDirectoryListing(boolean on) {
        GigiPreparedStatement update = DatabaseConnection.getInstance().prepare("UPDATE users SET listme = ? WHERE id = ?");
        update.setBoolean(1, on);
        update.setInt(2, getId());
        update.executeUpdate();
    }

    public void setContactInformation(String contactInfo) {
        GigiPreparedStatement update = DatabaseConnection.getInstance().prepare("UPDATE users SET contactinfo = ? WHERE id = ?");
        update.setString(1, contactInfo);
        update.setInt(2, getId());
        update.executeUpdate();
    }

    public boolean isInGroup(Group g) {
        return groups.contains(g);
    }

    public Set<Group> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public void grantGroup(User granter, Group toGrant) {
        groups.add(toGrant);
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO user_groups SET user=?, permission=?, grantedby=?");
        ps.setInt(1, getId());
        ps.setString(2, toGrant.getDatabaseName());
        ps.setInt(3, granter.getId());
        ps.execute();
    }

    public void revokeGroup(User revoker, Group toRevoke) {
        groups.remove(toRevoke);
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE user_groups SET deleted=CURRENT_TIMESTAMP, revokedby=? WHERE deleted is NULL AND permission=? AND user=?");
        ps.setInt(1, revoker.getId());
        ps.setString(2, toRevoke.getDatabaseName());
        ps.setInt(3, getId());
        ps.execute();
    }

    public List<Organisation> getOrganisations() {
        List<Organisation> orgas = new ArrayList<>();
        GigiPreparedStatement query = DatabaseConnection.getInstance().prepare("SELECT orgid FROM org_admin WHERE `memid`=? AND deleted is NULL");
        query.setInt(1, getId());
        GigiResultSet res = query.executeQuery();

        while (res.next()) {
            orgas.add(Organisation.getById(res.getInt(1)));
        }
        return orgas;
    }

    public static synchronized User getById(int id) {
        CertificateOwner co = CertificateOwner.getById(id);
        if (co instanceof User) {
            return (User) co;
        }
        return null;
    }

    public static User getByEmail(String mail) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT users.id FROM users inner join certOwners on certOwners.id=users.id WHERE email=? AND deleted is null");
        ps.setString(1, mail);
        GigiResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            return null;
        }
        return User.getById(rs.getInt(1));
    }

    public static User[] findByEmail(String mail) {
        LinkedList<User> results = new LinkedList<User>();
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT users.id FROM users inner join certOwners on certOwners.id=users.id WHERE users.email LIKE ? AND deleted is null GROUP BY users.id ASC LIMIT 100");
        ps.setString(1, mail);
        GigiResultSet rs = ps.executeQuery();
        while (rs.next()) {
            results.add(User.getById(rs.getInt(1)));
            System.out.println("Found user");
        }
        return results.toArray(new User[results.size()]);
    }

    public boolean canIssue(CertificateProfile p) {
        switch (p.getCAId()) {
        case 0:
            return true;
        case 1:
            return getAssurancePoints() > 50;
        case 2:
            return getAssurancePoints() > 50 && isInGroup(Group.getByString("codesigning"));
        case 3:
        case 4:
            return false; // has an orga
        default:
            return false;
        }
    }

}
