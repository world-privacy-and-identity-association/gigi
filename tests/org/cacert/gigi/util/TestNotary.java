package org.cacert.gigi.util;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Date;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.testUtils.BusinessTest;
import org.junit.Test;

public class TestNotary extends BusinessTest {

    // These tests create a lot of users and therefore require resetting of the
    // registering-rate-limit.
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
            Notary.assure(assurer, users[0], users[0].getPreferredName(), users[0].getDoB(), -1, "test-notary", "2014-01-01", AssuranceType.FACE_TO_FACE);
            fail("This shouldn't have passed");
        } catch (GigiApiException e) {
            // expected
        }
        for (int i = 0; i < result.length; i++) {
            assertEquals(result[i], assurer.getMaxAssurePoints());

            assuranceFail(assurer, users[i], result[i] + 1, "test-notary", "2014-01-01");
            Notary.assure(assurer, users[i], users[i].getPreferredName(), users[i].getDoB(), result[i], "test-notary", "2014-01-01", AssuranceType.FACE_TO_FACE);
            assuranceFail(assurer, users[i], result[i], "test-notary", "2014-01-01");
        }

        assertEquals(35, assurer.getMaxAssurePoints());

        assertEquals(User.EXPERIENCE_POINTS + (30 * User.EXPERIENCE_POINTS), assurer.getExperiencePoints());

    }

    private void assuranceFail(User assurer, User user, int i, String location, String date) throws SQLException {
        try {
            Notary.assure(assurer, user, user.getPreferredName(), user.getDoB(), i, location, date, AssuranceType.FACE_TO_FACE);
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
            assuranceFail(assurer, users[i], -1, "test-notary", "2014-01-01");
            assuranceFail(assurer, users[i], 11, "test-notary", "2014-01-01");
            if (User.POJAM_ENABLED) {
                Notary.assure(assurer, users[i], users[i].getPreferredName(), users[i].getDoB(), 10, "test-notary", "2014-01-01", AssuranceType.FACE_TO_FACE);
            }
            assuranceFail(assurer, users[i], 10, "test-notary", "2014-01-01");
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
        assuranceFail(assuranceUser, assuree, 10, null, "2014-01-01");
        // empty location
        assuranceFail(assuranceUser, assuree, 10, "", "2014-01-01");
        // date in the future
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", DateSelector.getDateFormat().format(new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)));
        // location too short
        assuranceFail(assuranceUser, assuree, 10, "n", "2014-01-01");
        // points too low
        assuranceFail(assuranceUser, assuree, -1, "notary-junit-test", "2014-01-01");
        // points too high
        assuranceFail(assuranceUser, assuree, 11, "notary-junit-test", "2014-01-01");

        // assure oneself
        assuranceFail(assuranceUser, assuranceUser, 10, "notary-junit-test", "2014-01-01");
        // not an assurer
        assuranceFail(assuree, assuranceUser, 10, "notary-junit-test", "2014-01-01");

        // valid
        Notary.assure(assuranceUser, assuree, assuree.getPreferredName(), assuree.getDoB(), 10, "notary-junit-test", "2014-01-01", AssuranceType.FACE_TO_FACE);

        // assure double
        assuranceFail(assuranceUser, assuree, 10, "notary-junit-test", "2014-01-01");

    }
}
