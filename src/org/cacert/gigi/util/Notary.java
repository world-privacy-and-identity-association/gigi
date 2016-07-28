package org.cacert.gigi.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.output.template.SprintfCommand;

public class Notary {

    // minimum date range between 2 verifications of the RA-Agent to the same
    // Applicant
    public final static int LIMIT_DAYS_VERIFICATION = TimeConditions.getInstance().getVerificationLimitDays();

    // maximum date range from date when the verification took place and the
    // entering to the system
    public final static int LIMIT_MAX_MONTHS_VERIFICATION = TimeConditions.getInstance().getVerificationMaxAgeMonths();

    public static void writeUserAgreement(User member, String document, String method, String comment, boolean active, int secmemid) {
        try (GigiPreparedStatement q = new GigiPreparedStatement("INSERT INTO `user_agreements` SET `memid`=?, `secmemid`=?," + " `document`=?,`date`=NOW(), `active`=?,`method`=?,`comment`=?")) {
            q.setInt(1, member.getId());
            q.setInt(2, secmemid);
            q.setString(3, document);
            q.setBoolean(4, active);
            q.setString(5, method);
            q.setString(6, comment);
            q.execute();
        }
    }

    public static boolean checkAssuranceIsPossible(User assurer, Name target) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `notary` where `to`=? and `from`=? and `method` = ? ::`notaryType` AND `deleted` IS NULL AND `when` > (now() - interval '1 days' * ?)")) {
            ps.setInt(1, target.getId());
            ps.setInt(2, assurer.getId());
            ps.setString(3, AssuranceType.FACE_TO_FACE.getDescription());
            ps.setInt(4, LIMIT_DAYS_VERIFICATION);
            GigiResultSet rs = ps.executeQuery();
            return !rs.next();
        }
    }

    public static final Group ASSURER_BLOCKED = Group.getByString("blockedassurer");

    public static final Group ASSUREE_BLOCKED = Group.getByString("blockedassuree");

    /**
     * This method assures another user.
     * 
     * @see User#canAssure() (for assurer)
     * @see #checkAssuranceIsPossible(User, User) (for assurer or assuree)
     * @param assurer
     *            the person that wants to assure
     * @param assuree
     *            the person that should be assured
     * @param assureeName
     *            the Name that was personally verified
     * @param dob
     *            the Date of birth that the assurer verified
     * @param awarded
     *            the points that should be awarded in total
     * @param location
     *            the location where the assurance took place
     * @param date
     *            the date when the assurance took place
     * @throws GigiApiException
     *             if the assurance fails (for various reasons)
     */
    public synchronized static void assure(User assurer, User assuree, Name assureeName, DayDate dob, int awarded, String location, String date, AssuranceType type) throws GigiApiException {
        may(assurer, assuree, AssuranceType.FACE_TO_FACE);
        GigiApiException gae = new GigiApiException();
        if ( !gae.isEmpty()) {
            throw gae;
        }
        if (date == null || date.equals("")) {
            gae.mergeInto(new GigiApiException("You must enter the date when you met the assuree."));
        } else {
            try {
                Date d = DateSelector.getDateFormat().parse(date);
                Calendar gc = GregorianCalendar.getInstance();
                gc.setTimeInMillis(System.currentTimeMillis());
                gc.add(Calendar.HOUR_OF_DAY, 12);
                if (d.getTime() > gc.getTimeInMillis()) {
                    gae.mergeInto(new GigiApiException("You must not enter a date in the future."));
                }
                gc.setTimeInMillis(System.currentTimeMillis());
                gc.add(Calendar.MONTH, -LIMIT_MAX_MONTHS_VERIFICATION);
                if (d.getTime() < gc.getTimeInMillis()) {
                    gae.mergeInto(new GigiApiException(SprintfCommand.createSimple("Verifications older than {0} months are not accepted.", LIMIT_MAX_MONTHS_VERIFICATION)));
                }
            } catch (ParseException e) {
                gae.mergeInto(new GigiApiException("You must enter the date in this format: YYYY-MM-DD."));
            }
        }
        // check location, min 3 characters
        if (location == null || location.equals("")) {
            gae.mergeInto(new GigiApiException("You failed to enter a location of your meeting."));
        } else if (location.length() <= 2) {
            gae.mergeInto(new GigiApiException("You must enter a location with at least 3 characters eg town and country."));
        }
        synchronized (assuree) {
            if (assurer.getId() == assuree.getId()) {
                throw new GigiApiException("You cannot verify yourself.");
            }
            if (assureeName.getOwner() != assuree) {
                throw new GigiApiException("Internal error, name does not belong to applicant.");
            }
            if ( !assurer.canAssure()) {
                throw new GigiApiException("You are not an RA-Agent.");
            }

            if ( !checkAssuranceIsPossible(assurer, assureeName)) {
                gae.mergeInto(new GigiApiException(SprintfCommand.createSimple("You have already verified this applicant within the last {0} days.", LIMIT_DAYS_VERIFICATION)));
            }

            if ( !assuree.getDoB().equals(dob)) {
                gae.mergeInto(new GigiApiException("The person you are assuring changed his personal details."));
            }

            if (awarded < 0) {
                gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
            } else {
                if (type == AssuranceType.NUCLEUS) {
                    if (awarded > 50) {
                        gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
                    }
                } else {
                    if (awarded > assurer.getMaxAssurePoints()) {
                        gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
                    }
                }
            }

            if ( !gae.isEmpty()) {
                throw gae;
            }

            if (type == AssuranceType.FACE_TO_FACE) {
                assureF2F(assurer, assuree, assureeName, awarded, location, date);
            } else if (type == AssuranceType.NUCLEUS) {
                assureNucleus(assurer, assuree, assureeName, awarded, location, date);
            } else if (type == AssuranceType.TTP_ASSISTED) {
                assureTTP(assurer, assuree, assureeName, awarded, location, date);
            } else {
                throw new GigiApiException(SprintfCommand.createSimple("Unknown Assurance type: {0}", type.toString()));
            }
            assurer.invalidateMadeAssurances();
            assuree.invalidateReceivedAssurances();
        }
    }

    private static void assureF2F(User assurer, User assuree, Name name, int awarded, String location, String date) throws GigiApiException {
        may(assurer, assuree, AssuranceType.FACE_TO_FACE);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?")) {
            ps.setInt(1, assurer.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.execute();
        }
    }

    private static void assureTTP(User assurer, User assuree, Name name, int awarded, String location, String date) throws GigiApiException {
        may(assurer, assuree, AssuranceType.TTP_ASSISTED);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `method`='TTP-Assisted'")) {
            ps.setInt(1, assurer.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.execute();
            assuree.revokeGroup(assurer, Group.TTP_APPLICANT);
        }
    }

    public static void may(User assurer, User assuree, AssuranceType t) throws GigiApiException {
        if (assuree.isInGroup(ASSUREE_BLOCKED)) {
            throw new GigiApiException("The applicant is blocked.");
        }
        if (assurer.isInGroup(ASSURER_BLOCKED)) {
            throw new GigiApiException("The RA Agent is blocked.");
        }

        if (t == AssuranceType.NUCLEUS) {
            if ( !assurer.isInGroup(Group.NUCLEUS_ASSURER)) {
                throw new GigiApiException("RA Agent needs to be Nucleus RA Agent.");
            }
            return;
        } else if (t == AssuranceType.TTP_ASSISTED) {
            if ( !assurer.isInGroup(Group.TTP_ASSURER)) {
                throw new GigiApiException("RA Agent needs to be TTP RA Agent.");
            }
            if ( !assuree.isInGroup(Group.TTP_APPLICANT)) {
                throw new GigiApiException("Applicant needs to be TTP Applicant.");
            }
            return;
        } else if (t == AssuranceType.FACE_TO_FACE) {
            return;
        }
        throw new GigiApiException("Verification type not possible.");
    }

    private static void assureNucleus(User assurer, User assuree, Name name, int awarded, String location, String date) throws GigiApiException {
        may(assurer, assuree, AssuranceType.NUCLEUS);
        // Do up to 35 points as f2f
        int f2fPoints = Math.min(assurer.getMaxAssurePoints(), awarded);
        assureF2F(assurer, assuree, name, f2fPoints, location, date);

        awarded -= f2fPoints;
        if (awarded <= 0) {
            return;
        }

        // Assure remaining points as "Nucleus Bonus"
        // Valid for 4 Weeks = 28 days
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `method`='Nucleus Bonus', `expire` = CURRENT_TIMESTAMP + interval '28 days'")) {
            ps.setInt(1, assurer.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.execute();
        }
    }

    public synchronized static void assureAll(User assurer, User assuree, DayDate dob, int awarded, String location, String date, AssuranceType type, Name[] toAssure) throws GigiApiException {
        if (toAssure.length == 0) {
            throw new GigiApiException("You must confirm at least one name to verify an account.");
        }
        boolean[] hadLessThan50Points = new boolean[toAssure.length];
        boolean hadTotalLessThan100 = assuree.getAssurancePoints() < 100;
        for (int i = 0; i < toAssure.length; i++) {
            hadLessThan50Points[i] = toAssure[i].getAssurancePoints() < 50;

            assure(assurer, assuree, toAssure[i], dob, awarded, location, date, type);
        }
        sendVerificationNotificationApplicant(assurer, assuree, toAssure, awarded, hadLessThan50Points, hadTotalLessThan100);
    }

    private static final MailTemplate verificationEntered = new MailTemplate(Notary.class.getResource("VerificationEntered.templ"));

    private static void sendVerificationNotificationApplicant(User assurer, User assuree, Name[] toAssure, final int awarded, final boolean[] hadLessThan50Points, boolean hadTotalLessThan100) {
        HashMap<String, Object> mailVars = new HashMap<>();
        mailVars.put("agent", assurer.getPreferredName().toString());
        mailVars.put("names", new ArrayIterable<Name>(toAssure) {

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                int totalVP = t.getAssurancePoints();
                vars.put("name", t.toString());
                vars.put("points", Integer.toString(awarded));
                vars.put("total", totalVP);
                if (totalVP < 50) {
                    vars.put("rem", (50 - totalVP));
                    vars.remove("gotGreater");
                } else if (hadLessThan50Points[i]) {
                    vars.put("gotGreater", true);
                    vars.remove("rem");
                }
            }

        });

        int grandTotalVP = assuree.getAssurancePoints();
        if (grandTotalVP >= 50 && grandTotalVP < 100) {
            mailVars.put("remAll", (100 - grandTotalVP));
            mailVars.remove("gotGreaterAll");
        } else if (hadTotalLessThan100) {
            mailVars.put("gotGreaterAll", true);
            mailVars.remove("remAll");
        }
        try {
            verificationEntered.sendMail(Language.getInstance(assuree.getPreferredLocale()), mailVars, assuree.getEmail());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
