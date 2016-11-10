package org.cacert.gigi.dbObjects;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.Timestamp;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.dbObjects.NamePart.NamePartType;
import org.cacert.gigi.testUtils.BusinessTest;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.junit.Test;

public class TestAssuranceMail extends BusinessTest {

    private User agent;

    private User applicant;

    private Name firstName;

    private Name secondName;

    private Name thirdName;

    private String message;

    private void newAgent() throws GigiApiException {
        agent = User.getById(createAssuranceUser("Marianne", "Mustermann", createUniqueName() + "@example.com", TEST_PASSWORD));
    }

    private void newApplicant() throws GigiApiException {
        applicant = User.getById(createVerifiedUser("John", "Doe", createUniqueName() + "@example.com", TEST_PASSWORD));
        firstName = applicant.getPreferredName();
        secondName = new Name(applicant, new NamePart(NamePartType.FIRST_NAME, "James"), new NamePart(NamePartType.LAST_NAME, "Doe"));
        thirdName = new Name(applicant, new NamePart(NamePartType.FIRST_NAME, "James"), new NamePart(NamePartType.FIRST_NAME, "John"), new NamePart(NamePartType.LAST_NAME, "Doe"));
    }

    private void raiseXP(User agentXP, int recurring) throws GigiApiException {
        for (int i = 0; i < recurring; i++) {
            String applicantT = createUniqueName() + "@example.com";
            int applicantId = createVerifiedUser("John", "Doe", applicantT, TEST_PASSWORD);
            User applicantXP = User.getById(applicantId);
            applicantXP = User.getById(applicantId);
            Notary.assure(agentXP, applicantXP, applicantXP.getNames()[0], applicantXP.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        }
    }

    private void enterVerification(Name... names) throws GigiApiException {
        enterVerification(10, names);
    }

    private void enterVerification(int points, Name... names) throws GigiApiException {
        Notary.assureAll(agent, applicant, applicant.getDoB(), points, createUniqueName(), validVerificationDateString(), AssuranceType.FACE_TO_FACE, names, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
    }

    private void enterVerificationInPast(int points, Name name) {

        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, agent.getId());
            ps.setInt(2, name.getId());
            ps.setInt(3, points);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 200));
            ps.execute();
        }
    }

    @Test
    public void testVerificationFirstApplicant() throws GigiApiException {
        newApplicant();
        newAgent();

        // verify preferred name only
        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // verification first two names
        newAgent();

        enterVerification(firstName, secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString("James Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // verification all three names
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString("James Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString("James John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

        // New verification preferred name
        newAgent();

        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));

        // verification all three names reaches 50 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("James Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString("James John Doe: with 10 to total 20 Verification Points." + "\n" + requiresMore(30)));
        assertThat(message, containsString(requiresMoreTotal(50)));

        // verification all three names reaches 60 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));
        assertThat(message, containsString("James John Doe: with 10 to total 30 Verification Points." + "\n" + requiresMore(20)));
        assertThat(message, containsString(requiresMoreTotal(40)));

        // verification all three names reaches 70 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("James John Doe: with 10 to total 40 Verification Points." + "\n" + requiresMore(10)));
        assertThat(message, containsString(requiresMoreTotal(30)));

        // verification all three names reaches 80 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 80 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(20)));

        // verification all three names reaches 90 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 90 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 60 Verification Points."));
        assertThat(message, containsString(requiresMoreTotal(10)));

        // verification all three names reaches 100 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 100 Verification Points."));
        assertThat(message, containsString("James Doe: with 10 to total 80 Verification Points."));
        assertThat(message, containsString("James John Doe: with 10 to total 70 Verification Points."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));

        // verification all three names reaches 100 VP
        newAgent();

        enterVerification(firstName, secondName, thirdName);
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
    public void testVerificationSecondApplicant() throws GigiApiException {
        newApplicant();

        // verify preferred name only 5 times
        newAgent();
        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(firstName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(firstName);

        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(50)));

        // verify preferred name second name
        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));
        assertThat(message, containsString(requiresMoreTotal(40)));

        // verify preferred name second name 4 more times
        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();

        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));

        // get more than 100 VP in total
        newAgent();
        enterVerification(secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James Doe: with 10 to total 60 Verification Points."));

        // verify third name
        newAgent();
        enterVerification(thirdName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("James John Doe: with 10 to total 10 Verification Points." + "\n" + requiresMore(40)));

    }

    @Test
    public void testVerificationMultiple() throws GigiApiException {
        newApplicant();

        // verify with 35 VP
        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 5, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        Notary.assure(agent, applicant, applicant.getNames()[1], applicant.getDoB(), 5, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        // add first Verification in the past result first name 45 VP
        newAgent();
        raiseXP(agent, 5);
        enterVerificationInPast(10, firstName);

        // add second Verification result first name 50 VP
        enterVerification(15, firstName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 15 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString(requiresMoreTotal(50)));

        // verify first name to 85 VP
        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 10, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        newAgent();
        Notary.assure(agent, applicant, applicant.getNames()[0], applicant.getDoB(), 5, "Test location", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));

        // add first Verification in the past result first name 95 VP
        newAgent();
        raiseXP(agent, 5);
        enterVerificationInPast(10, firstName);
        enterVerificationInPast(10, secondName);

        // add second Verification result first name 100 VP, second name 50 VP
        enterVerification(15, firstName, secondName);
        message = getMailReceiver().receive().getMessage();
        assertThat(message, containsString("RA-Agent Marianne Mustermann verified your name(s):"));
        assertThat(message, containsString("John Doe: with 15 to total 100 Verification Points."));
        assertThat(message, containsString("James Doe: with 15 to total 50 Verification Points." + "\n" + "You can now issue client certificates with this name."));
        assertThat(message, containsString("You can now apply for RA Agent status or code signing ability."));
    }
}
