package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.testUtils.BusinessTest;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.Notary;

public class TestVerification extends BusinessTest {

    private final Timestamp yesterday = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY);

    private final Timestamp tomorrow = new Timestamp(System.currentTimeMillis() + DayDate.MILLI_DAY);

    /**
     * at least 27 months ago, so is outside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min27month = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 27 * 31);

    /**
     * at least 24 months ago (but less than 27), so is inside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min24month = new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 24 * 31);

    private int agentID;

    private int agent2ID;

    private int applicantID;

    private int applicantNameID;

    private User applicant;

    private int applicantMultID;

    public TestVerification() throws GigiApiException {

    }

    // test for verification in 39 month period
    private void enterVerification(int agentID, int applicantID) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");

            ps.execute();
        }
    }

    private void enterVerificationExpired(int agentID, int applicantID, Timestamp expired) {
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

    private void enterVerificationWhen(int agentID, int applicantID, Timestamp when) {
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

    private void enterVericationWhen(int agentID, int applicantID, Timestamp when, int points) {
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

    private void enterVerificationDeleted(int agentID, int applicantID, Timestamp deleted) {
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
        agentID = createVerificationUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
        agent2ID = createVerificationUser("a", "d", createUniqueName() + "@example.com", TEST_PASSWORD);
        applicantID = createVerifiedUser("a", "c", createUniqueName() + "@example.com", TEST_PASSWORD);
        applicant = User.getById(applicantID);
        applicantNameID = User.getById(applicantID).getPreferredName().getId();
        applicantMultID = createVerifiedUser("a", "e", createUniqueName() + "@example.com", TEST_PASSWORD);
    }

    @Test
    public void testVerificationYesterday() throws IOException {
        enterVerificationWhen(agentID, applicantNameID, yesterday);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testApprox24MonthAgo() throws IOException {
        enterVerificationWhen(agentID, applicantNameID, min24month);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testApprox39MonthAgo() throws IOException {
        enterVerificationWhen(agentID, applicantNameID, min27month);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testTomorrowExpired() throws IOException {
        enterVerificationExpired(agentID, applicantNameID, tomorrow);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testYesterdayExpired() throws IOException {
        enterVerificationExpired(agentID, applicantNameID, yesterday);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testNormal() throws IOException {
        enterVerification(agentID, applicantNameID);
        assertTrue(applicant.isInVerificationLimit());
    }

    @Test
    public void testDeletedYesterday() throws IOException {
        enterVerificationDeleted(agentID, applicantNameID, yesterday);
        assertFalse(applicant.isInVerificationLimit());
    }

    @Test
    public void testMultipleVerificationPossible() throws IOException {
        User agent = User.getById(agentID);
        User applicantMult = User.getById(applicantMultID);

        enterVerificationWhen(agentID, applicantMult.getPreferredName().getId(), min27month);

        // test that new entry would be possible
        assertTrue(Notary.checkVerificationIsPossible(agent, applicantMult.getPreferredName()));

        // enter new entry
        enterVerificationWhen(agentID, applicantMult.getPreferredName().getId(), yesterday);

        // test that new entry is not possible
        assertFalse(Notary.checkVerificationIsPossible(agent, applicantMult.getPreferredName()));

    }

    @Test
    public void testMultipleVerificationPointsCalculation() throws IOException {

        User agent = User.getById(agentID);
        User applicantMult = User.getById(applicantMultID);

        enterVerificationWhen(agentID, applicantMult.getPreferredName().getId(), min27month);
        int xPoints = agent.getExperiencePoints();

        // test that VP after first entry

        assertEquals(applicantMult.getVerificationPoints(), 10);

        // enter second entry to check correct calculation with larger points
        enterVericationWhen(agentID, applicantMult.getPreferredName().getId(), min24month, 20);
        assertEquals(applicantMult.getVerificationPoints(), 20);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter third entry to check correct calculation with less points
        enterVericationWhen(agentID, applicantMult.getPreferredName().getId(), yesterday, 15);
        assertEquals(applicantMult.getVerificationPoints(), 15);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter expired entry
        enterVerificationExpired(agentID, applicantMult.getPreferredName().getId(), yesterday);
        assertEquals(applicantMult.getVerificationPoints(), 15);

        // enter deleted entry same agent
        enterVerificationDeleted(agentID, applicantMult.getPreferredName().getId(), yesterday);
        assertEquals(applicantMult.getVerificationPoints(), 15);

        // enter expired entry future
        enterVerificationExpired(agentID, applicantMult.getPreferredName().getId(), tomorrow);
        assertEquals(applicantMult.getVerificationPoints(), 10);

        // test correct XP calculation
        assertEquals(agent.getExperiencePoints(), xPoints);

        // enter entry from different agent
        enterVerificationWhen(agent2ID, applicantMult.getPreferredName().getId(), yesterday);
        assertEquals(applicantMult.getVerificationPoints(), 20);

        // enter entry for second applicant
        enterVerificationWhen(agentID, applicant.getPreferredName().getId(), yesterday);

        assertEquals(agent.getExperiencePoints(), xPoints + User.EXPERIENCE_POINTS);
    }
}
