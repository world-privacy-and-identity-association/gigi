package club.wpia.gigi.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Verification.VerificationType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.ArrayIterable;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.output.template.SprintfCommand;

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

    public static boolean checkVerificationIsPossible(User agent, Name target) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `notary` where `to`=? and `from`=? and `method` = ? ::`notaryType` AND `deleted` IS NULL AND `when` > (now() - interval '1 days' * ?::INTEGER)")) {
            ps.setInt(1, target.getId());
            ps.setInt(2, agent.getId());
            ps.setEnum(3, VerificationType.FACE_TO_FACE);
            ps.setInt(4, LIMIT_DAYS_VERIFICATION);
            GigiResultSet rs = ps.executeQuery();
            return !rs.next();
        }
    }

    public static final Group AGENT_BLOCKED = Group.BLOCKED_AGENT;

    public static final Group APPLICANT_BLOCKED = Group.BLOCKED_APPLICANT;

    public static final Group VERIFY_NOTIFICATION = Group.VERIFY_NOTIFICATION;

    /**
     * This method verifies another user.
     * 
     * @see User#canVerify() (for agent)
     * @see #checkVerificationIsPossible(User, User) (for agent or applicant)
     * @param agent
     *            the person that wants to verify
     * @param applicant
     *            the person that should be verified
     * @param applicantName
     *            the Name that was personally verified
     * @param dob
     *            the Date of birth that the agent verified
     * @param awarded
     *            the points that should be awarded in total
     * @param location
     *            the location where the verification took place
     * @param date
     *            the date when the verification took place
     * @throws GigiApiException
     *             if the verification fails (for various reasons)
     */
    public synchronized static void verify(User agent, User applicant, Name applicantName, DayDate dob, int awarded, String location, String date, VerificationType type, Country country) throws GigiApiException {
        may(agent, applicant, VerificationType.FACE_TO_FACE);
        GigiApiException gae = new GigiApiException();
        if ( !gae.isEmpty()) {
            throw gae;
        }
        if (date == null || date.equals("")) {
            gae.mergeInto(new GigiApiException("You must enter the date when you met the applicant."));
        } else {
            try {
                DayDate d = new DayDate(DateSelector.getDateFormat().parse(date).getTime());
                if (d.start().getTime() > System.currentTimeMillis()) {
                    gae.mergeInto(new GigiApiException("You must not enter a date in the future."));
                }
                Calendar gc = GregorianCalendar.getInstance();
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

        if (country == null) {
            gae.mergeInto(new GigiApiException("You failed to enter the country of your meeting."));
        }

        synchronized (applicant) {
            if (agent.getId() == applicant.getId()) {
                throw new GigiApiException("You cannot verify yourself.");
            }
            if (applicantName.getOwner() != applicant) {
                throw new GigiApiException("Internal error, name does not belong to applicant.");
            }
            if ( !agent.canVerify()) {
                throw new GigiApiException("You are not an RA-Agent.");
            }

            if ( !checkVerificationIsPossible(agent, applicantName)) {
                gae.mergeInto(new GigiApiException(SprintfCommand.createSimple("You have already verified this applicant within the last {0} days.", LIMIT_DAYS_VERIFICATION)));
            }

            if ( !applicant.getDoB().equals(dob)) {
                gae.mergeInto(new GigiApiException("The person you are verifying changed his personal details."));
            }

            if (awarded < 0) {
                gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
            } else {
                if (type == VerificationType.NUCLEUS) {
                    if (awarded > 50) {
                        gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
                    }
                } else {
                    if (awarded > agent.getMaxVerifyPoints()) {
                        gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
                    }
                }
            }

            if ( !gae.isEmpty()) {
                throw gae;
            }

            if (type == VerificationType.FACE_TO_FACE) {
                verifyF2F(agent, applicant, applicantName, awarded, location, date, country);
            } else if (type == VerificationType.NUCLEUS) {
                verifyNucleus(agent, applicant, applicantName, awarded, location, date, country);
            } else if (type == VerificationType.TTP_ASSISTED) {
                verifyTTP(agent, applicant, applicantName, awarded, location, date, country);
            } else {
                throw new GigiApiException(SprintfCommand.createSimple("Unknown Verification type: {0}", type.toString()));
            }
            agent.invalidateMadeVerifications();
            applicant.invalidateReceivedVerifications();
        }
    }

    private static void verifyF2F(User agent, User applicant, Name name, int awarded, String location, String date, Country country) throws GigiApiException {
        may(agent, applicant, VerificationType.FACE_TO_FACE);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `country`=?")) {
            ps.setInt(1, agent.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.setString(6, country.getCode());
            ps.execute();
        }
    }

    private static void verifyTTP(User agent, User applicant, Name name, int awarded, String location, String date, Country country) throws GigiApiException {
        may(agent, applicant, VerificationType.TTP_ASSISTED);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `country`=?, `method`='TTP-Assisted'")) {
            ps.setInt(1, agent.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.setString(6, country.getCode());
            ps.execute();
            applicant.revokeGroup(agent, Group.TTP_APPLICANT);
        }
    }

    public static void may(User agent, User applicant, VerificationType t) throws GigiApiException {
        if (applicant.isInGroup(APPLICANT_BLOCKED)) {
            throw new GigiApiException("The applicant is blocked.");
        }
        if (agent.isInGroup(AGENT_BLOCKED)) {
            throw new GigiApiException("The RA Agent is blocked.");
        }

        if (t == VerificationType.NUCLEUS) {
            if ( !agent.isInGroup(Group.NUCLEUS_AGENT)) {
                throw new GigiApiException("RA Agent needs to be Nucleus RA Agent.");
            }
            return;
        } else if (t == VerificationType.TTP_ASSISTED) {
            if ( !agent.isInGroup(Group.TTP_AGENT) || !agent.hasValidTTPAgentChallenge()) {
                throw new GigiApiException("RA Agent needs to be TTP RA Agent and have a valid TTP RA Agent Challenge.");
            }
            if ( !applicant.isInGroup(Group.TTP_APPLICANT)) {
                throw new GigiApiException("Applicant needs to be TTP Applicant.");
            }
            return;
        } else if (t == VerificationType.FACE_TO_FACE) {
            return;
        }
        throw new GigiApiException("Verification type not possible.");
    }

    private static void verifyNucleus(User agent, User applicant, Name name, int awarded, String location, String date, Country country) throws GigiApiException {
        may(agent, applicant, VerificationType.NUCLEUS);
        // Do up to 35 points as f2f
        int f2fPoints = Math.min(agent.getMaxVerifyPoints(), awarded);
        verifyF2F(agent, applicant, name, f2fPoints, location, date, country);

        awarded -= f2fPoints;
        if (awarded <= 0) {
            return;
        }

        // Verify remaining points as "Nucleus Bonus"
        // Valid for 4 Weeks = 28 days
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `country`=?, `method`='Nucleus Bonus', `expire` = CURRENT_TIMESTAMP + interval '28 days'")) {
            ps.setInt(1, agent.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, awarded);
            ps.setString(4, location);
            ps.setString(5, date);
            ps.setString(6, country.getCode());
            ps.execute();
        }
    }

    public synchronized static void verifyAll(User agent, User applicant, DayDate dob, int awarded, String location, String date, VerificationType type, Name[] toVerify, Country country) throws GigiApiException {
        if (toVerify.length == 0) {
            throw new GigiApiException("You must confirm at least one name to verify an account.");
        }
        boolean[] hadLessThan50Points = new boolean[toVerify.length];
        boolean hadTotalLessThan100 = applicant.getVerificationPoints() < 100;
        for (int i = 0; i < toVerify.length; i++) {
            hadLessThan50Points[i] = toVerify[i].getVerificationPoints() < 50;

            verify(agent, applicant, toVerify[i], dob, awarded, location, date, type, country);
        }
        sendVerificationNotificationApplicant(agent, applicant, toVerify, awarded, hadLessThan50Points, hadTotalLessThan100);
        if (agent.isInGroup(VERIFY_NOTIFICATION)) {
            sendVerificationNotificationAgent(agent, applicant, toVerify, awarded, location, date, country);
        }
    }

    private static final MailTemplate verificationEntered = new MailTemplate(Notary.class.getResource("VerificationEntered.templ"));

    private static final MailTemplate verificationAgentEntered = new MailTemplate(Notary.class.getResource("VerificationAgentEntered.templ"));

    private static void sendVerificationNotificationApplicant(User agent, User applicant, Name[] toVerify, final int awarded, final boolean[] hadLessThan50Points, boolean hadTotalLessThan100) {
        HashMap<String, Object> mailVars = new HashMap<>();
        mailVars.put("agent", agent.getPreferredName().toString());
        mailVars.put("names", new ArrayIterable<Name>(toVerify) {

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                int totalVP = t.getVerificationPoints();
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

        int grandTotalVP = applicant.getVerificationPoints();
        if (grandTotalVP >= 50 && grandTotalVP < 100) {
            mailVars.put("remAll", (100 - grandTotalVP));
            mailVars.remove("gotGreaterAll");
        } else if (hadTotalLessThan100) {
            mailVars.put("gotGreaterAll", true);
            mailVars.remove("remAll");
        }
        try {
            verificationEntered.sendMail(Language.getInstance(applicant.getPreferredLocale()), mailVars, applicant.getEmail());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendVerificationNotificationAgent(User agent, User applicant, Name[] toVerify, final int awarded, String location, String date, Country country) {
        HashMap<String, Object> mailVars = new HashMap<>();
        mailVars.put("email", applicant.getEmail());
        mailVars.put("location", location);
        mailVars.put("date", date);
        mailVars.put("country", country.getName());
        mailVars.put("points", Integer.toString(awarded));
        mailVars.put("names", new ArrayIterable<Name>(toVerify) {

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                vars.put("name", t.toString());
            }

        });

        try {
            verificationAgentEntered.sendMail(Language.getInstance(applicant.getPreferredLocale()), mailVars, agent.getEmail());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
