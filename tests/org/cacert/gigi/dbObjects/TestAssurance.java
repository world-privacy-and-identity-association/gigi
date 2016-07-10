package org.cacert.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.testUtils.BusinessTest;
import org.junit.Test;

public class TestAssurance extends BusinessTest {

    private final Timestamp yesterday = new Timestamp(System.currentTimeMillis() - 24L * 60 * 60 * 1000L);

    private final Timestamp tomorrow = new Timestamp(System.currentTimeMillis() + 24L * 60 * 60 * 1000L);

    /**
     * at least 39 months ago, so is outside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min39month = new Timestamp(System.currentTimeMillis() - 24L * 60 * 60 * 39 * 31 * 1000L);

    /**
     * at least 24 months ago (but less than 39), so is inside the window of
     * {@link User#VERIFICATION_MONTHS}
     */
    private final Timestamp min24month = new Timestamp(System.currentTimeMillis() - 24L * 60 * 60 * 24 * 31 * 1000L);

    private final int agentID;

    private final int applicantID;

    public TestAssurance() throws GigiApiException {
        agentID = createAssuranceUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
        applicantID = createVerifiedUser("a", "c", createUniqueName() + "@example.com", TEST_PASSWORD);
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

    @Test
    public void testVerificationYesterday() throws IOException {
        enterAssuranceWhen(agentID, applicantID, yesterday);
        assertTrue(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testApprox24MonthAgo() throws IOException {
        enterAssuranceWhen(agentID, applicantID, min24month);
        assertTrue(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testApprox39MonthAgo() throws IOException {
        enterAssuranceWhen(agentID, applicantID, min39month);
        assertFalse(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testTomorrowExpired() throws IOException {
        enterAssuranceExpired(agentID, applicantID, tomorrow);
        assertTrue(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testYesterdayExpired() throws IOException {
        enterAssuranceExpired(agentID, applicantID, yesterday);
        assertFalse(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testNormal() throws IOException {
        enterAssurance(agentID, applicantID);
        assertTrue(User.isInVerificationLimit(applicantID));
    }

    @Test
    public void testDeletedYesterday() throws IOException {
        enterAssuranceDeleted(agentID, applicantID, yesterday);
        assertFalse(User.isInVerificationLimit(applicantID));
    }
}
