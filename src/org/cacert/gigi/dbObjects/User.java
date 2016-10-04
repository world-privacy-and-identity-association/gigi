package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.CATS.CATSType;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.util.CalendarUtil;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PasswordHash;
import org.cacert.gigi.util.PasswordStrengthChecker;
import org.cacert.gigi.util.TimeConditions;

/**
 * Represents an acting, assurable, user. Synchronizing on user means: no
 * name-change and no assurance.
 */
public class User extends CertificateOwner {

    private static final long serialVersionUID = -7915843843752264176L;

    private DayDate dob;

    private String email;

    private Assurance[] receivedAssurances;

    private Assurance[] madeAssurances;

    private Locale locale;

    private Set<Group> groups = new HashSet<>();

    public static final int MINIMUM_AGE = 16;

    public static final int MAXIMUM_PLAUSIBLE_AGE = 120;

    public static final int POJAM_AGE = 14;

    public static final int ADULT_AGE = 18;

    public static final boolean POJAM_ENABLED = false;

    public static final int EXPERIENCE_POINTS = 4;

    /**
     * Time in months a verification is considered "recent".
     */
    public static final int VERIFICATION_MONTHS = TimeConditions.getInstance().getVerificationMonths();

    private Name preferredName;

    private Country residenceCountry;

    protected User(GigiResultSet rs) throws GigiApiException {
        super(rs.getInt("id"));

        dob = new DayDate(rs.getDate("dob"));
        email = rs.getString("email");
        preferredName = Name.getById(rs.getInt("preferredName"));

        if (rs.getString("country") != null) {
            residenceCountry = Country.getCountryByCode(rs.getString("Country"), Country.CountryCodeType.CODE_2_CHARS);
        }

        String localeStr = rs.getString("language");
        if (localeStr == null || localeStr.equals("")) {
            locale = Locale.getDefault();
        } else {
            locale = Language.getLocaleFromString(localeStr);
        }

        refreshGroups();
    }

    public synchronized void refreshGroups() {
        HashSet<Group> hs = new HashSet<>();
        try (GigiPreparedStatement psg = new GigiPreparedStatement("SELECT `permission` FROM `user_groups` WHERE `user`=? AND `deleted` is NULL")) {
            psg.setInt(1, getId());

            try (GigiResultSet rs2 = psg.executeQuery()) {
                while (rs2.next()) {
                    hs.add(Group.getByString(rs2.getString(1)));
                }
            }
        }
        groups = hs;
    }

    public User(String email, String password, DayDate dob, Locale locale, Country residenceCountry, NamePart... preferred) throws GigiApiException {
        this.email = email;
        this.dob = dob;
        this.locale = locale;
        this.preferredName = new Name(this, preferred);
        try (GigiPreparedStatement query = new GigiPreparedStatement("INSERT INTO `users` SET `email`=?, `password`=?, `dob`=?, `language`=?, id=?, `preferredName`=?, `country` = ?")) {
            query.setString(1, email);
            query.setString(2, PasswordHash.hash(password));
            query.setDate(3, dob.toSQLDate());
            query.setString(4, locale.toString());
            query.setInt(5, getId());
            query.setInt(6, preferredName.getId());
            query.setString(7, residenceCountry == null ? null : residenceCountry.getCode());
            query.execute();
        }
        new EmailAddress(this, email, locale);
    }

    public Name[] getNames() {
        try (GigiPreparedStatement gps = new GigiPreparedStatement("SELECT `id` FROM `names` WHERE `uid`=? AND `deleted` IS NULL", true)) {
            gps.setInt(1, getId());
            return fetchNamesToArray(gps);
        }
    }

    public Name[] getNonDeprecatedNames() {
        try (GigiPreparedStatement gps = new GigiPreparedStatement("SELECT `id` FROM `names` WHERE `uid`=? AND `deleted` IS NULL AND `deprecated` IS NULL", true)) {
            gps.setInt(1, getId());
            return fetchNamesToArray(gps);
        }
    }

    private Name[] fetchNamesToArray(GigiPreparedStatement gps) {
        GigiResultSet rs = gps.executeQuery();
        rs.last();
        Name[] dt = new Name[rs.getRow()];
        rs.beforeFirst();
        for (int i = 0; rs.next(); i++) {
            dt[i] = Name.getById(rs.getInt(1));
        }
        return dt;
    }

    public DayDate getDoB() {
        return dob;
    }

    public void setDoB(DayDate dob) throws GigiApiException {
        synchronized (Notary.class) {
            if (getReceivedAssurances().length != 0) {
                throw new GigiApiException("No change after verification allowed.");
            }

            if ( !CalendarUtil.isOfAge(dob, User.MINIMUM_AGE)) {
                throw new GigiApiException("Entered date of birth is below the restricted age requirements.");
            }

            if (CalendarUtil.isOfAge(dob, User.MAXIMUM_PLAUSIBLE_AGE)) {
                throw new GigiApiException("Entered date of birth exceeds the maximum age set in our policies. Please check your DoB is correct and contact support if the issue persists.");
            }
            this.dob = dob;
            rawUpdateUserData();
        }

    }

    protected void setDoBAsSupport(DayDate dob) throws GigiApiException {
        synchronized (Notary.class) {
            this.dob = dob;
            rawUpdateUserData();
        }

    }

    public String getEmail() {
        return email;
    }

    public void changePassword(String oldPass, String newPass) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `password` FROM `users` WHERE `id`=?")) {
            ps.setInt(1, getId());
            try (GigiResultSet rs = ps.executeQuery()) {
                if ( !rs.next()) {
                    throw new GigiApiException("User not found... very bad.");
                }
                if (PasswordHash.verifyHash(oldPass, rs.getString(1)) == null) {
                    throw new GigiApiException("Old password does not match.");
                }
            }
        }
        setPassword(newPass);
    }

    private void setPassword(String newPass) throws GigiApiException {
        PasswordStrengthChecker.assertStrongPassword(newPass, getNames(), getEmail());
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE users SET `password`=? WHERE id=?")) {
            ps.setString(1, PasswordHash.hash(newPass));
            ps.setInt(2, getId());
            ps.executeUpdate();
        }
    }

    public boolean canAssure() {
        if (POJAM_ENABLED) {
            if ( !CalendarUtil.isOfAge(dob, POJAM_AGE)) { // PoJAM
                return false;
            }
        } else {
            if ( !CalendarUtil.isOfAge(dob, ADULT_AGE)) {
                return false;
            }
        }
        if (getAssurancePoints() < 100) {
            return false;
        }

        return hasPassedCATS();

    }

    public boolean hasPassedCATS() {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT 1 FROM `cats_passed` where `user_id`=? AND `variant_id`=?")) {
            query.setInt(1, getId());
            query.setInt(2, CATSType.ASSURER_CHALLENGE.getId());
            try (GigiResultSet rs = query.executeQuery()) {
                if (rs.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public int getAssurancePoints() {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT SUM(lastpoints) FROM ( SELECT DISTINCT ON (`from`) `from`, `points` as lastpoints FROM `notary` INNER JOIN `names` ON `names`.`id`=`to` WHERE `notary`.`deleted` is NULL AND (`expire` IS NULL OR `expire` > CURRENT_TIMESTAMP) AND `names`.`uid` = ? ORDER BY `from`, `when` DESC) as p")) {
            query.setInt(1, getId());

            GigiResultSet rs = query.executeQuery();
            int points = 0;

            if (rs.next()) {
                points = rs.getInt(1);
            }

            return points;
        }
    }

    public int getExperiencePoints() {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT count(*) FROM ( SELECT `names`.`uid` FROM `notary` INNER JOIN `names` ON `names`.`id` = `to` WHERE `from`=? AND `notary`.`deleted` IS NULL AND `method` = ? ::`notaryType` GROUP BY `names`.`uid`) as p")) {
            query.setInt(1, getId());
            query.setEnum(2, AssuranceType.FACE_TO_FACE);

            GigiResultSet rs = query.executeQuery();
            int points = 0;

            if (rs.next()) {
                points = rs.getInt(1) * EXPERIENCE_POINTS;
            }

            return points;
        }
    }

    /**
     * Gets the maximum allowed points NOW. Note that an assurance needs to
     * re-check PoJam as it has taken place in the past.
     * 
     * @return the maximal points @
     */
    @SuppressWarnings("unused")
    public int getMaxAssurePoints() {
        if ( !CalendarUtil.isOfAge(dob, ADULT_AGE) && POJAM_ENABLED) {
            return 10; // PoJAM
        }

        int exp = getExperiencePoints();
        int points = 10;

        if (exp >= 5 * EXPERIENCE_POINTS) {
            points += 5;
        }
        if (exp >= 10 * EXPERIENCE_POINTS) {
            points += 5;
        }
        if (exp >= 15 * EXPERIENCE_POINTS) {
            points += 5;
        }
        if (exp >= 20 * EXPERIENCE_POINTS) {
            points += 5;
        }
        if (exp >= 25 * EXPERIENCE_POINTS) {
            points += 5;
        }

        return points;
    }

    public boolean isValidName(String name) {
        for (Name n : getNames()) {
            if (n.matches(name) && n.getAssurancePoints() >= 50) {
                return true;
            }
        }
        return false;
    }

    public void updateDefaultEmail(EmailAddress newMail) throws GigiApiException {
        for (EmailAddress email : getEmails()) {
            if (email.getAddress().equals(newMail.getAddress())) {
                if ( !email.isVerified()) {
                    throw new GigiApiException("Email not verified.");
                }

                try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE users SET email=? WHERE id=?")) {
                    ps.setString(1, newMail.getAddress());
                    ps.setInt(2, getId());
                    ps.execute();
                }

                this.email = newMail.getAddress();
                return;
            }
        }

        throw new GigiApiException("Given address not an address of the user.");
    }

    public void deleteEmail(EmailAddress delMail) throws GigiApiException {
        if (getEmail().equals(delMail.getAddress())) {
            throw new GigiApiException("Can't delete user's default e-mail.");
        }

        for (EmailAddress email : getEmails()) {
            if (email.getId() == delMail.getId()) {
                try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `emails` SET `deleted`=CURRENT_TIMESTAMP WHERE `id`=?")) {
                    ps.setInt(1, delMail.getId());
                    ps.execute();
                }
                return;
            }
        }
        throw new GigiApiException("Email not one of user's email addresses.");
    }

    public synchronized Assurance[] getReceivedAssurances() {
        if (receivedAssurances == null) {
            try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT * FROM `notary` INNER JOIN `names` ON `names`.`id` = `notary`.`to` WHERE `names`.`uid`=? AND `notary`.`deleted` IS NULL ORDER BY `when` DESC")) {
                query.setInt(1, getId());

                GigiResultSet res = query.executeQuery();
                List<Assurance> assurances = new LinkedList<Assurance>();

                while (res.next()) {
                    assurances.add(assuranceByRes(res));
                }

                this.receivedAssurances = assurances.toArray(new Assurance[0]);
            }
        }

        return receivedAssurances;
    }

    public synchronized Assurance[] getMadeAssurances() {
        if (madeAssurances == null) {
            try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT * FROM notary WHERE `from`=? AND deleted is NULL ORDER BY `when` DESC")) {
                query.setInt(1, getId());

                try (GigiResultSet res = query.executeQuery()) {
                    List<Assurance> assurances = new LinkedList<Assurance>();

                    while (res.next()) {
                        assurances.add(assuranceByRes(res));
                    }

                    this.madeAssurances = assurances.toArray(new Assurance[0]);
                }
            }
        }

        return madeAssurances;
    }

    public synchronized void invalidateMadeAssurances() {
        madeAssurances = null;
    }

    public synchronized void invalidateReceivedAssurances() {
        receivedAssurances = null;
    }

    private void rawUpdateUserData() {
        try (GigiPreparedStatement update = new GigiPreparedStatement("UPDATE users SET dob=? WHERE id=?")) {
            update.setDate(1, getDoB().toSQLDate());
            update.setInt(2, getId());
            update.executeUpdate();
        }
    }

    public Locale getPreferredLocale() {
        return locale;
    }

    public void setPreferredLocale(Locale locale) {
        this.locale = locale;

    }

    public synchronized Name getPreferredName() {
        return preferredName;
    }

    public synchronized void setPreferredName(Name preferred) throws GigiApiException {
        if (preferred.getOwner() != this) {
            throw new GigiApiException("Cannot set a name as preferred one that does not belong to this account.");
        }
        this.preferredName = preferred;
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `users` SET `preferredName`=? WHERE `id`=?")) {
            ps.setInt(1, preferred.getId());
            ps.setInt(2, getId());
            ps.executeUpdate();
        }

    }

    public boolean isInGroup(Group g) {
        return groups.contains(g);
    }

    public Set<Group> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public void grantGroup(User granter, Group toGrant) throws GigiApiException {
        if (toGrant.isManagedBySupport() && !granter.isInGroup(Group.SUPPORTER)) {
            throw new GigiApiException("Group may only be managed by supporter");
        }
        if (toGrant.isManagedBySupport() && granter == this) {
            throw new GigiApiException("Group may only be managed by supporter that is not oneself");
        }
        groups.add(toGrant);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `user_groups` SET `user`=?, `permission`=?::`userGroup`, `grantedby`=?")) {
            ps.setInt(1, getId());
            ps.setEnum(2, toGrant);
            ps.setInt(3, granter.getId());
            ps.execute();
        }
    }

    public void revokeGroup(User revoker, Group toRevoke) throws GigiApiException {
        if (toRevoke.isManagedBySupport() && !revoker.isInGroup(Group.SUPPORTER)) {
            throw new GigiApiException("Group may only be managed by supporter");
        }
        groups.remove(toRevoke);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `user_groups` SET `deleted`=CURRENT_TIMESTAMP, `revokedby`=? WHERE `deleted` IS NULL AND `permission`=?::`userGroup` AND `user`=?")) {
            ps.setInt(1, revoker.getId());
            ps.setEnum(2, toRevoke);
            ps.setInt(3, getId());
            ps.execute();
        }
    }

    public List<Organisation> getOrganisations() {
        return getOrganisations(false);
    }

    public List<Organisation> getOrganisations(boolean isAdmin) {
        List<Organisation> orgas = new ArrayList<>();
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT `orgid` FROM `org_admin` WHERE `memid`=? AND `deleted` IS NULL" + (isAdmin ? " AND master='y'" : ""))) {
            query.setInt(1, getId());
            try (GigiResultSet res = query.executeQuery()) {
                while (res.next()) {
                    orgas.add(Organisation.getById(res.getInt(1)));
                }

                return orgas;
            }
        }
    }

    public static synchronized User getById(int id) {
        CertificateOwner co = CertificateOwner.getById(id);
        if (co instanceof User) {
            return (User) co;
        }

        return null;
    }

    public static User getByEmail(String mail) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `users`.`id` FROM `users` INNER JOIN `certOwners` ON `certOwners`.`id` = `users`.`id` WHERE `email`=? AND `deleted` IS NULL")) {
            ps.setString(1, mail);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return null;
            }

            return User.getById(rs.getInt(1));
        }
    }

    public static User[] findByEmail(String mail) {
        LinkedList<User> results = new LinkedList<User>();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `users`.`id` FROM `users` INNER JOIN `certOwners` ON `certOwners`.`id` = `users`.`id` WHERE `users`.`email` LIKE ? AND `deleted` IS NULL GROUP BY `users`.`id` LIMIT 100")) {
            ps.setString(1, mail);
            GigiResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(User.getById(rs.getInt(1)));
            }
            return results.toArray(new User[results.size()]);
        }
    }

    public EmailAddress[] getEmails() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `emails` WHERE `memid`=? AND `deleted` IS NULL")) {
            ps.setInt(1, getId());

            GigiResultSet rs = ps.executeQuery();
            LinkedList<EmailAddress> data = new LinkedList<EmailAddress>();

            while (rs.next()) {
                data.add(EmailAddress.getById(rs.getInt(1)));
            }

            return data.toArray(new EmailAddress[0]);
        }
    }

    @Override
    public boolean isValidEmail(String email) {
        for (EmailAddress em : getEmails()) {
            if (em.getAddress().equals(email)) {
                return em.isVerified();
            }
        }

        return false;
    }

    public String[] getTrainings() {
        try (GigiPreparedStatement prep = new GigiPreparedStatement("SELECT `pass_date`, `type_text`, `language`, `version` FROM `cats_passed` LEFT JOIN `cats_type` ON `cats_type`.`id`=`cats_passed`.`variant_id`  WHERE `user_id`=? ORDER BY `pass_date` ASC")) {
            prep.setInt(1, getId());
            GigiResultSet res = prep.executeQuery();
            List<String> entries = new LinkedList<String>();

            while (res.next()) {
                StringBuilder training = new StringBuilder();
                training.append(DateSelector.getDateFormat().format(res.getTimestamp(1)));
                training.append(" (");
                training.append(res.getString(2));
                if (res.getString(3).length() > 0) {
                    training.append(" ");
                    training.append(res.getString(3));
                }
                if (res.getString(4).length() > 0) {
                    training.append(", ");
                    training.append(res.getString(4));
                }
                training.append(")");
                entries.add(training.toString());
            }

            return entries.toArray(new String[0]);
        }

    }

    public int generatePasswordResetTicket(User actor, String token, String privateToken) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `passwordResetTickets` SET `memid`=?, `creator`=?, `token`=?, `private_token`=?")) {
            ps.setInt(1, getId());
            ps.setInt(2, getId());
            ps.setString(3, token);
            ps.setString(4, PasswordHash.hash(privateToken));
            ps.execute();
            return ps.lastInsertId();
        }
    }

    public static User getResetWithToken(int id, String token) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `memid` FROM `passwordResetTickets` WHERE `id`=? AND `token`=? AND `used` IS NULL AND `created` > CURRENT_TIMESTAMP - interval '1 hours' * ?")) {
            ps.setInt(1, id);
            ps.setString(2, token);
            ps.setInt(3, PasswordResetPage.HOUR_MAX);
            GigiResultSet res = ps.executeQuery();
            if ( !res.next()) {
                return null;
            }
            return User.getById(res.getInt(1));
        }
    }

    public synchronized void consumePasswordResetTicket(int id, String private_token, String newPassword) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `private_token` FROM `passwordResetTickets` WHERE `id`=? AND `memid`=? AND `used` IS NULL")) {
            ps.setInt(1, id);
            ps.setInt(2, getId());
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                throw new GigiApiException("Token could not be found, has already been used, or is expired.");
            }
            if (PasswordHash.verifyHash(private_token, rs.getString(1)) == null) {
                throw new GigiApiException("Private token does not match.");
            }
            setPassword(newPassword);
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `passwordResetTickets` SET  `used` = CURRENT_TIMESTAMP WHERE `id`=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Assurance assuranceByRes(GigiResultSet res) {
        try {
            return new Assurance(res.getInt("id"), User.getById(res.getInt("from")), Name.getById(res.getInt("to")), res.getString("location"), res.getString("method"), res.getInt("points"), res.getString("date"), res.getString("country") == null ? null : Country.getCountryByCode(res.getString("country"), CountryCodeType.CODE_2_CHARS));
        } catch (GigiApiException e) {
            throw new Error(e);
        }
    }

    public boolean isInVerificationLimit() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `notary` INNER JOIN `names` ON `names`.`id`=`to` WHERE `names`.`uid` = ? AND `when` > (now() - (interval '1 month' * ?)) AND (`expire` IS NULL OR `expire` > now()) AND `notary`.`deleted` IS NULL;")) {
            ps.setInt(1, getId());
            ps.setInt(2, VERIFICATION_MONTHS);

            GigiResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {}

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {}

    public Country getResidenceCountry() {
        return residenceCountry;
    }

    public void setResidenceCountry(Country residenceCountry) {
        this.residenceCountry = residenceCountry;
        rawUpdateCountryData();
    }

    private void rawUpdateCountryData() {
        try (GigiPreparedStatement update = new GigiPreparedStatement("UPDATE users SET country=? WHERE id=?")) {
            update.setString(1, residenceCountry == null ? null : residenceCountry.getCode());
            update.setInt(2, getId());
            update.executeUpdate();
        }
    }
}
