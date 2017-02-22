package club.wpia.gigi.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.CATS;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Verification.VerificationType;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.testUtils.BusinessTest;

public class TestNotary extends BusinessTest {

    public final Country DE = Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS);

    public TestNotary() throws GigiApiException {}

    @Test
    public void testNormalVerification() throws SQLException, GigiApiException {
        User[] users = new User[30];
        for (int i = 0; i < users.length; i++) {
            int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName() + "@email.org", TEST_PASSWORD);
            users[i] = User.getById(id);
        }
        User agent = User.getById(createVerificationUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        int[] result = new int[] {
                10, 10, 10, 10, 15, 15, 15, 15, 15, 20, 20, 20, 20, 20, 25, 25, 25, 25, 25, 30, 30, 30, 30, 30, 35, 35, 35, 35, 35, 35
        };

        try {
            Notary.verify(agent, users[0], users[0].getPreferredName(), users[0].getDoB(), -1, "test-notary", validVerificationDateString(), VerificationType.FACE_TO_FACE, DE);
            fail("This shouldn't have passed");
        } catch (GigiApiException e) {
            // expected
        }
        for (int i = 0; i < result.length; i++) {
            assertEquals(result[i], agent.getMaxVerifyPoints());

            verificationFail(agent, users[i], result[i] + 1, "test-notary", validVerificationDateString());
            Notary.verify(agent, users[i], users[i].getPreferredName(), users[i].getDoB(), result[i], "test-notary", validVerificationDateString(), VerificationType.FACE_TO_FACE, DE);
            verificationFail(agent, users[i], result[i], "test-notary", validVerificationDateString());
        }

        assertEquals(35, agent.getMaxVerifyPoints());

        assertEquals(User.EXPERIENCE_POINTS + (30 * User.EXPERIENCE_POINTS), agent.getExperiencePoints());

    }

    private void verificationFail(User agent, User applicant, int i, String location, String date) throws SQLException {
        try {
            Notary.verify(agent, applicant, applicant.getPreferredName(), applicant.getDoB(), i, location, date, VerificationType.FACE_TO_FACE, DE);
            fail("This shouldn't have passed");
        } catch (GigiApiException e) {
            // expected
        }
    }

    @Test
    public void testPoJam() throws SQLException, GigiApiException {
        User[] users = new User[30];
        for (int i = 0; i < users.length; i++) {
            int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName() + "@email.org", TEST_PASSWORD);
            users[i] = User.getById(id);
        }
        int id = createVerificationUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `users` SET dob=NOW() - interval '15 years' WHERE id=?")) {
            ps.setInt(1, id);
            ps.execute();
        }
        ObjectCache.clearAllCaches(); // reload values from db
        User agent = User.getById(id);
        for (int i = 0; i < users.length; i++) {
            verificationFail(agent, users[i], -1, "test-notary", validVerificationDateString());
            verificationFail(agent, users[i], 11, "test-notary", validVerificationDateString());
            if (User.POJAM_ENABLED) {
                Notary.verify(agent, users[i], users[i].getPreferredName(), users[i].getDoB(), 10, "test-notary", validVerificationDateString(), VerificationType.FACE_TO_FACE, DE);
            }
            verificationFail(agent, users[i], 10, "test-notary", validVerificationDateString());
        }
    }

    @Test
    public void testFail() throws SQLException, GigiApiException {
        User agent = User.getById(createVerificationUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        User applicant = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));

        // invalid date format
        verificationFail(agent, applicant, 10, "notary-junit-test", "2014-01-blah");
        // empty date
        verificationFail(agent, applicant, 10, "notary-junit-test", "");
        // null date
        verificationFail(agent, applicant, 10, "notary-junit-test", null);
        // null location
        verificationFail(agent, applicant, 10, null, validVerificationDateString());
        // empty location
        verificationFail(agent, applicant, 10, "", validVerificationDateString());
        // date in the future
        verificationFail(agent, applicant, 10, "notary-junit-test", DateSelector.getDateFormat().format(new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)));
        // location too short
        verificationFail(agent, applicant, 10, "n", validVerificationDateString());
        // points too low
        verificationFail(agent, applicant, -1, "notary-junit-test", validVerificationDateString());
        // points too high
        verificationFail(agent, applicant, 11, "notary-junit-test", validVerificationDateString());

        // verify oneself
        verificationFail(agent, agent, 10, "notary-junit-test", validVerificationDateString());
        // not an agent
        verificationFail(applicant, agent, 10, "notary-junit-test", validVerificationDateString());

        // valid
        Notary.verify(agent, applicant, applicant.getPreferredName(), applicant.getDoB(), 10, "notary-junit-test", validVerificationDateString(), VerificationType.FACE_TO_FACE, DE);

        // verify double
        verificationFail(agent, applicant, 10, "notary-junit-test", validVerificationDateString());

    }

    @Test
    public void testNucleus() throws SQLException, GigiApiException, IOException {
        User agent = User.getById(createVerificationUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        agent.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User applicant = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        Name n1 = applicant.getPreferredName();
        Name n2 = new Name(applicant, new NamePart(NamePartType.FIRST_NAME, "F2"), new NamePart(NamePartType.LAST_NAME, "L2"));

        assertEquals(0, applicant.getExperiencePoints());
        assertEquals(User.EXPERIENCE_POINTS, agent.getExperiencePoints());
        assertEquals(0, applicant.getVerificationPoints());
        assertEquals(0, n2.getVerificationPoints());
        Notary.verifyAll(agent, applicant, applicant.getDoB(), 50, "notary-junit-test", validVerificationDateString(), VerificationType.NUCLEUS, new Name[] {
                n1, n2
        }, DE);
        assertEquals(0, applicant.getExperiencePoints());
        assertEquals(2 * User.EXPERIENCE_POINTS, agent.getExperiencePoints());
        assertEquals(50, applicant.getVerificationPoints());
        assertEquals(50, n1.getVerificationPoints());
        assertEquals(50, n2.getVerificationPoints());
    }

    @Test
    public void testNucleusProcess() throws SQLException, GigiApiException, IOException {
        User agent1 = User.getById(createVerificationUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        agent1.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User agent2 = User.getById(createVerificationUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        agent2.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User applicant = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        Notary.verify(agent1, applicant, applicant.getPreferredName(), applicant.getDoB(), 50, "test", validVerificationDateString(), VerificationType.NUCLEUS, DE);
        Notary.verify(agent2, applicant, applicant.getPreferredName(), applicant.getDoB(), 50, "test", validVerificationDateString(), VerificationType.NUCLEUS, DE);

        assertEquals(100, applicant.getVerificationPoints());
        assertFalse(applicant.canVerify());
        CATS.enterResult(applicant, CATSType.AGENT_CHALLENGE, new Date(), "de", "1");
        assertTrue(applicant.canVerify());
    }
}
