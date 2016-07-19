package org.cacert.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.testUtils.BusinessTest;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.junit.Before;
import org.junit.Test;

public class TestAssurance extends BusinessTest {

    private final Timestamp yesterday = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY);

    private final Timestamp tomorrow = new Timestamp(System.currentTimeMillis() + DayDate.MILLI_DAY);

    /**
     * at least 39 months ago, so is outside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min39month = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 39 * 31);

    /**
     * at least 24 months ago (but less than 39), so is inside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min24month = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 24 * 31);

    private int agentID;

    private int agent2ID;

    private int applicantID;

    private User applicant;

    private int applicantMultID;

    public TestAssurance() throws GigiApiException {

    }

    // test for verification in 39 month period
    private void enterAssurance(int agentID, int applicantID) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");

            ps.execute();
        }
    }

    private void enterAssuranceExpired(int agentID, int applicantID, Timestamp expired) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `expire`=? ")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, expired);
            ps.execute();
        }
    }

    private void enterAssuranceWhen(int agentID, int applicantID, Timestamp when) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, when);
            ps.execute();
        }
    }

    private void enterAssuranceWhen(int agentID, int applicantID, Timestamp when, int points) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, points);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, when);
            ps.execute();
        }
    }

    private void enterAssuranceDeleted(int agentID, int applicantID, Timestamp deleted) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `deleted`=? ")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, deleted);
            ps.execute();
        }
    }

    @Before
    public void initTest() throws GigiApiException {
        agentID = createAssuranceUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
        agent2ID = createAssuranceUser("a", "d", createUniqueName() + "@example.com", TEST_PASSWORD);
        applicantID = createVerifiedUser("a", "c", createUniqueName() + "@example.com", TEST_PASSWORD);
        applicant = User.getById(applicantID);
        applicantMultID = createVerifiedUser("a", "e", createUniqueName() + "@example.com", TEST_PASSWORD);
    }

    @Test
    public void testVerificationYesterday() throws IOException {
        enterAssuranceWhen(agentID, applicantID, yesterday);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testApprox24MonthAgo() throws IOException {
        enterAssuranceWhen(agentID, applicantID, min24month);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testApprox39MonthAgo() throws IOException {
        enterAssuranceWhen(agentID, applicantID, min39month);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testTomorrowExpired() throws IOException {
        enterAssuranceExpired(agentID, applicantID, tomorrow);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testYesterdayExpired() throws IOException {
        enterAssuranceExpired(agentID, applicantID, yesterday);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testNormal() throws IOException {
        enterAssurance(agentID, applicantID);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testDeletedYesterday() throws IOException {
        enterAssuranceDeleted(agentID, applicantID, yesterday);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testMultipleAssurancePossible() throws IOException {

        User agent = User.getById(agentID);
        User applicantMult = User.getById(applicantMultID);

        enterAssuranceWhen(agentID, applicantMultID, min39month);

        // test that new entry would be possible
        try {
            Notary.checkAssuranceIsPossible(agent, applicantMult);
        } catch (GigiApiException e) {
            assertTrue(false);
        }

        // enter new entry
        enterAssuranceWhen(agentID, applicantMultID, yesterday);

        // test that new entry is not possible
        try {
            Notary.checkAssuranceIsPossible(agent, applicantMult);
        } catch (GigiApiException e) {
            assertTrue(true);
        }

    }

    @Test
    public void testMultipleAssurancePointsCalculation() throws IOException {

        User agent = User.getById(agentID);
        User applicantMult = User.getById(applicantMultID);

        enterAssuranceWhen(agentID, applicantMultID, min39month);

        int xPoints = agent.getExperiencePoints();

        // test that VP after first entry

        assertEquals(applicantMult.getAssurancePoints(), 10);

        // enter second entry to check correct calculation with larger points
        enterAssuranceWhen(agentID, applicantMultID, min24month, 20);
        assertEquals(applicantMult.getAssurancePoints(), 20);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter third entry to check correct calculation with less points
        enterAssuranceWhen(agentID, applicantMultID, yesterday, 15);
        assertEquals(applicantMult.getAssurancePoints(), 15);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter expired entry
        enterAssuranceExpired(agentID, applicantMultID, yesterday);
        assertEquals(applicantMult.getAssurancePoints(), 15);

        // enter deleted entry same agent
        enterAssuranceDeleted(agentID, applicantMultID, yesterday);
        assertEquals(applicantMult.getAssurancePoints(), 15);

        // enter expired entry future
        enterAssuranceExpired(agentID, applicantMultID, tomorrow);
        assertEquals(applicantMult.getAssurancePoints(), 10);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter entry from different agent
        enterAssuranceWhen(agent2ID, applicantMultID, yesterday);
        assertEquals(applicantMult.getAssurancePoints(), 20);

        // enter entry for second applicant
        enterAssuranceWhen(agentID, applicantID, yesterday);
        assertEquals(agent.getExperiencePoints(), xPoints + 2);

    }
}
