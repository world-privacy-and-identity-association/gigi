package org.cacert.gigi;

import java.sql.SQLException;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
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
        u.setPreferredLocale(Locale.ENGLISH);
        long dob = System.currentTimeMillis();
        dob -= dob % (1000 * 60 * 60 * 24);
        u.setDob(new java.sql.Date(dob));
        u.setEmail(createUniqueName() + "a@email.org");
        u.insert("password");
        int id = u.getId();
        User u2 = User.getById(id);
        assertEquals(u, u2);
    }

    @Test
    public void testWebStoreAndLoad() throws SQLException {
        int id = createVerifiedUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
        assertEquals("aä", u.getFname());
        assertEquals("b", u.getLname());
        assertEquals("", u.getMname());
    }

    @Test
    public void testAssurerUtilMethods() throws SQLException {
        int id = createAssuranceUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
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

    @Test
    public void testMatcherMethods() throws SQLException, GigiApiException {
        String uq = createUniqueName();
        int id = createVerifiedUser("aä", "b", uq + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
        new EmailAddress(u, uq + "b@email.org").insert(Language.getInstance(Locale.ENGLISH));
        new EmailAddress(u, uq + "c@email.org").insert(Language.getInstance(Locale.ENGLISH));
        new Domain(u, uq + "a.testdomain.org").insert();
        new Domain(u, uq + "b.testdomain.org").insert();
        new Domain(u, uq + "c.testdomain.org").insert();
        assertEquals(3, u.getEmails().length);
        assertEquals(3, u.getDomains().length);
        assertTrue(u.isValidDomain(uq + "a.testdomain.org"));
        assertTrue(u.isValidDomain(uq + "b.testdomain.org"));
        assertTrue(u.isValidDomain(uq + "c.testdomain.org"));
        assertTrue(u.isValidDomain("a." + uq + "a.testdomain.org"));
        assertTrue(u.isValidDomain("*." + uq + "a.testdomain.org"));
        assertFalse(u.isValidDomain("a" + uq + "a.testdomain.org"));
        assertFalse(u.isValidDomain("b" + uq + "a.testdomain.org"));

        assertTrue(u.isValidEmail(uq + "a@email.org"));
        assertTrue(u.isValidEmail(uq + "b@email.org"));
        assertFalse(u.isValidEmail(uq + "b+6@email.org"));
        assertFalse(u.isValidEmail(uq + "b*@email.org"));

        assertTrue(u.isValidName("aä b"));
        assertFalse(u.isValidName("aä c"));
        assertFalse(u.isValidName("aä d b"));

    }

}
