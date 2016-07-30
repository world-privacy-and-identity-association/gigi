package org.cacert.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.sql.Timestamp;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.junit.Test;

public class TestAssuranceMail extends ManagedTest {

    private String agentM;

    private String applicantM;

    private String cookieAgent;

    private String cookieApplicant;

    private User agent;

    private User applicant;

    private int firstName;

    private int secondName;

    private int thirdName;

    private String message;

    private void newAgent() throws IOException {
        agentM = createUniqueName() + "@example.com";
        int agentID = createAssuranceUser("Marianne", "Mustermann", agentM, TEST_PASSWORD);
        agent = User.getById(agentID);
        cookieAgent = login(agentM, TEST_PASSWORD);

    }

    private void newApplicant() throws IOException {
        applicantM = createUniqueName() + "@example.com";
        int applicantId = createVerifiedUser("John", "Doe", applicantM, TEST_PASSWORD);
        cookieApplicant = login(applicantM, TEST_PASSWORD);
        executeBasicWebInteraction(cookieApplicant, MyDetails.PATH, "fname=James&lname=Doe&action=addName", 0);
        executeBasicWebInteraction(cookieApplicant, MyDetails.PATH, "fname=James+John&lname=Doe&action=addName", 0);
        applicant = User.getById(applicantId);
        Name[] names = applicant.getNames();
        firstName = 0;
        secondName = 0;
        thirdName = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i].toString().equals("John Doe")) {
                firstName = names[i].getId();
            }
            if (names[i].toString().equals("James Doe")) {
                secondName = names[i].getId();
            }
            if (names[i].toString().equals("James John Doe")) {
                thirdName = names[i].getId();
            }
        }
        assertNotEquals(0, firstName);
        assertNotEquals(0, secondName);
        assertNotEquals(0, thirdName);
    }

    private void raiseXP(User agentXP, int recurring) throws GigiApiException {
        for (int i = 0; i < recurring; i++) {
            String applicantT = createUniqueName() + "@example.com";
            int applicantId = createVerifiedUser("John", "Doe", applicantT, TEST_PASSWORD);
            User applicantXP = User.getById(applicantId);
            applicantXP = User.getById(applicantId);
            Notary.assure(agentXP, applicantXP, applicantXP.getNames()[0], applicantXP.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);
        }
    }

    private String enterVerification(String query) throws MalformedURLException, IOException {
        return enterVerification(query, 10);

    }

    private String enterVerification(String query, int points) throws MalformedURLException, IOException {
        URLConnection uc = TestAssurance.buildupAssureFormConnection(cookieAgent, applicant.getEmail(), true);
        uc.getOutputStream().write((query + "&date=" + validVerificationDateString() + "&location=" + createUniqueName() + "&certify=1&rules=1&assertion=1&points=" + points).getBytes("UTF-8"));
        uc.getOutputStream().flush();
        return IOUtils.readURL(uc);

    }

    private void enterVerificationInPast(int points, int nameId) {

        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, agent.getId());
            ps.setInt(2, nameId);
            ps.setInt(3, points);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 200));
            ps.execute();
        }
    }

    @Test
    public void testVerificationFirstApplicant() throws MalformedURLException, IOException {
        clearCaches();
        newApplicant();
        newAgent();

        // verify preferred name only
        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // verification first two names
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString("James Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // verification all three names
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString("James Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString("James John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // New verification preferred name
        newAgent();

        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));

        // verification all three names reaches 50 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("James Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString("James John Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString(requiresMoreTotal(50)));

        // verification all three names reaches 60 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));
        assertThat(message, containsString("James John Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString(requiresMoreTotal(40)));

        // verification all three names reaches 70 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("James John Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));
        assertThat(message, containsString(requiresMoreTotal(30)));

        // verification all three names reaches 80 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 80 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(20)));

        // verification all three names reaches 90 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 90 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString(requiresMoreTotal(10)));

        // verification all three names reaches 100 VP
        clearCaches();
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 100 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 80 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));

        // verification all three names reaches 100 VP
        newAgent();

        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName + "&assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 110 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 90 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 80 Verification Points."));
    }

    private String requiresMore(int points) {
        return "To issue client certificates with this name you need " + points + " more Verification Points.";
    }

    private String requiresMoreTotal(int points) {
        return "To apply for RA Agent status or code signing ability you need " + points + " more Verification Points.";
    }

    @Test
    public void testVerificationSecondApplicant() throws MalformedURLException, IOException {
        clearCaches();
        newApplicant();

        // verify preferred name only 5 times
        newAgent();
        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + firstName);

        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(50)));

        clearCaches();

        // verify preferred name second name
        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));
        assertThat(message, containsString(requiresMoreTotal(40)));

        // verify preferred name second name 4 more times
        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));

        // get more than 100 VP in total
        newAgent();
        enterVerification("assuredName=" + secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 60 Verification Points."));

        // verify third name
        newAgent();
        enterVerification("assuredName=" + thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

    }

    @Test
    public void testVerificationMultiple() throws MalformedURLException, IOException, GigiApiException {
        clearCaches();
        newApplicant();

        // verify with 35 VP
        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 5, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 5, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        // add first Verification in the past result first name 45 VP
        newAgent();
        raiseXP(agent, 5);
        enterVerificationInPast(10, firstName);

        // add second Verification result first name 50 VP
        enterVerification("assuredName=" + firstName, 15);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 15 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(50)));

        // verify first name to 85 VP
        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 5, "Test location", "2014-11-06", AssuranceType.FACE_TO_FACE);

        // add first Verification in the past result first name 95 VP
        newAgent();
        raiseXP(agent, 5);
        enterVerificationInPast(10, firstName);
        enterVerificationInPast(10, secondName);

        // add second Verification result first name 100 VP, second name 50 VP
        enterVerification("assuredName=" + firstName + "&assuredName=" + secondName, 15);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 15 to total 100 Verification Points."));
        assertThat(message, containsString("James Doe: with 15 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));
    }
}
