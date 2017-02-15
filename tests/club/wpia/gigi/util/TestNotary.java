package club.wpia.gigi.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.CATS;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Assurance.AssuranceType;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.testUtils.BusinessTest;
import club.wpia.gigi.util.Notary;

public class TestNotary extends BusinessTest {

    public final Country DE = Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS);

    public TestNotary() throws GigiApiException {}

    @Test
    public void testNormalAssurance() throws SQLException, GigiApiException {
        User[] users = new User[30];
        for (int i = 0; i < users.length; i++) {
            int id = createVerifiedUser("fn" + i, "ln" + i, createUniqueName() + "@email.org", TEST_PASSWORD);
            users[i] = User.getById(id);
        }
        User assurer = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        int[] result = new int[] {
                10, 10, 10, 10, 15, 15, 15, 15, 15, 20, 20, 20, 20, 20, 25, 25, 25, 25, 25, 30, 30, 30, 30, 30, 35, 35, 35, 35, 35, 35
        };

        try {
            Notary.assure(assurer, users[0], users[0].getPreferredName(), users[0].getDoB(), -1, "test-notary", validVerificationDateString(), AssuranceType.FACE_TO_FACE, DE);
            fail("This shouldn't have passed");
        } catch (GigiApiException e) {
            // expected
        }
        for (int i = 0; i < result.length; i++) {
            assertEquals(result[i], assurer.getMaxAssurePoints());

            assuranceFail(assurer, users[i], result[i] + 1, "test-notary", validVerificationDateString());
            Notary.assure(assurer, users[i], users[i].getPreferredName(), users[i].getDoB(), result[i], "test-notary", validVerificationDateString(), AssuranceType.FACE_TO_FACE, DE);
            assuranceFail(assurer, users[i], result[i], "test-notary", validVerificationDateString());
        }

        assertEquals(35, assurer.getMaxAssurePoints());

        assertEquals(User.EXPERIENCE_POINTS + (30 * User.EXPERIENCE_POINTS), assurer.getExperiencePoints());

    }

    private void assuranceFail(User assurer, User user, int i, String location, String date) throws SQLException {
        try {
            Notary.assure(assurer, user, user.getPreferredName(), user.getDoB(), i, location, date, AssuranceType.FACE_TO_FACE, DE);
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
        int id = createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `users` SET dob=NOW() - interval '15 years' WHERE id=?")) {
            ps.setInt(1, id);
            ps.execute();
        }
        ObjectCache.clearAllCaches(); // reload values from db
        User assurer = User.getById(id);
        for (int i = 0; i < users.length; i++) {
            assuranceFail(assurer, users[i], -1, "test-notary", validVerificationDateString());
            assuranceFail(assurer, users[i], 11, "test-notary", validVerificationDateString());
            if (User.POJAM_ENABLED) {
                Notary.assure(assurer, users[i], users[i].getPreferredName(), users[i].getDoB(), 10, "test-notary", validVerificationDateString(), AssuranceType.FACE_TO_FACE, DE);
            }
            assuranceFail(assurer, users[i], 10, "test-notary", validVerificationDateString());
        }
    }

    @Test
    public void testFail() throws SQLException, GigiApiException {
        User assuranceUser = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        User assuree = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));

        // invalid date format
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", "2014-01-blah");
        // empty date
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", "");
        // null date
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", null);
        // null location
        assuranceFail(assuranceUser, assuree, 10, null, validVerificationDateString());
        // empty location
        assuranceFail(assuranceUser, assuree, 10, "", validVerificationDateString());
        // date in the future
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", DateSelector.getDateFormat().format(new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)));
        // location too short
        assuranceFail(assuranceUser, assuree, 10, "n", validVerificationDateString());
        // points too low
        assuranceFail(assuranceUser, assuree, -1, "notary-junit-test", validVerificationDateString());
        // points too high
        assuranceFail(assuranceUser, assuree, 11, "notary-junit-test", validVerificationDateString());

        // verify oneself
        assuranceFail(assuranceUser, assuranceUser, 10, "notary-junit-test", validVerificationDateString());
        // not an assurer
        assuranceFail(assuree, assuranceUser, 10, "notary-junit-test", validVerificationDateString());

        // valid
        Notary.assure(assuranceUser, assuree, assuree.getPreferredName(), assuree.getDoB(), 10, "notary-junit-test", validVerificationDateString(), AssuranceType.FACE_TO_FACE, DE);

        // verify double
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", validVerificationDateString());

    }

    @Test
    public void testNucleus() throws SQLException, GigiApiException, IOException {
        User assuranceUser = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        assuranceUser.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User assuree = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        Name n1 = assuree.getPreferredName();
        Name n2 = new Name(assuree, new NamePart(NamePartType.FIRST_NAME, "F2"), new NamePart(NamePartType.LAST_NAME, "L2"));

        assertEquals(0, assuree.getExperiencePoints());
        assertEquals(User.EXPERIENCE_POINTS, assuranceUser.getExperiencePoints());
        assertEquals(0, assuree.getAssurancePoints());
        assertEquals(0, n2.getAssurancePoints());
        Notary.assureAll(assuranceUser, assuree, assuree.getDoB(), 50, "notary-junit-test", validVerificationDateString(), AssuranceType.NUCLEUS, new Name[] {
                n1, n2
        }, DE);
        assertEquals(0, assuree.getExperiencePoints());
        assertEquals(2 * User.EXPERIENCE_POINTS, assuranceUser.getExperiencePoints());
        assertEquals(50, assuree.getAssurancePoints());
        assertEquals(50, n1.getAssurancePoints());
        assertEquals(50, n2.getAssurancePoints());
    }

    @Test
    public void testNucleusProcess() throws SQLException, GigiApiException, IOException {
        User assuranceUser = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        assuranceUser.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User assuranceUser2 = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        assuranceUser2.grantGroup(getSupporter(), Group.NUCLEUS_ASSURER);
        User assuree = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.org", TEST_PASSWORD));
        Notary.assure(assuranceUser, assuree, assuree.getPreferredName(), assuree.getDoB(), 50, "test", validVerificationDateString(), AssuranceType.NUCLEUS, DE);
        Notary.assure(assuranceUser2, assuree, assuree.getPreferredName(), assuree.getDoB(), 50, "test", validVerificationDateString(), AssuranceType.NUCLEUS, DE);

        assertEquals(100, assuree.getAssurancePoints());
        assertFalse(assuree.canAssure());
        CATS.enterResult(assuree, CATSType.ASSURER_CHALLENGE, new Date(), "de", "1");
        assertTrue(assuree.canAssure());
    }
}
