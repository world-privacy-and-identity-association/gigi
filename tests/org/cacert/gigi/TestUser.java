package org.cacert.gigi;

import java.sql.SQLException;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestUser extends ManagedTest {

    @Test
    public void testStoreAndLoad() throws SQLException {
        User u = new User();
        u.setFname("user");
        u.setLname("last");
        u.setMname("");
        u.setSuffix("");
        long dob = System.currentTimeMillis();
        dob -= dob % (1000 * 60 * 60 * 24);
        u.setDob(new java.sql.Date(dob));
        u.setEmail(createUniqueName() + "a@email.org");
        u.insert("password");
        int id = u.getId();
        User u2 = new User(id);
        assertEquals(u, u2);
    }

    @Test
    public void testWebStoreAndLoad() throws SQLException {
        int id = createVerifiedUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        User u = new User(id);
        assertEquals("aä", u.getFname());
        assertEquals("b", u.getLname());
        assertEquals("", u.getMname());
    }

    @Test
    public void testAssurerUtilMethods() throws SQLException {
        int id = createAssuranceUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        User u = new User(id);
        assertTrue(u.canAssure());
        int assurancePoints = u.getAssurancePoints();
        int expPoints = u.getExperiencePoints();
        assertEquals(100, assurancePoints);
        assertEquals(2, expPoints);
        assertTrue(u.hasPassedCATS());
        assertEquals(10, u.getMaxAssurePoints());

        assertEquals("aä", u.getFname());
        assertEquals("b", u.getLname());
        assertEquals("", u.getMname());
    }
}
