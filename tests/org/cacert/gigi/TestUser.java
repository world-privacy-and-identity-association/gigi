package org.cacert.gigi;

import static org.junit.Assert.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Assurance;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

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
        assertEquals(u.getName(), u2.getName());
        assertEquals(u.getDob().toString(), u2.getDob().toString());
        assertEquals(u.getEmail(), u2.getEmail());
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
        new Domain(u, uq + "a-testdomain.org").insert();
        new Domain(u, uq + "b-testdomain.org").insert();
        new Domain(u, uq + "c-testdomain.org").insert();
        assertEquals(3, u.getEmails().length);
        assertEquals(3, u.getDomains().length);
        assertTrue(u.isValidDomain(uq + "a-testdomain.org"));
        assertTrue(u.isValidDomain(uq + "b-testdomain.org"));
        assertTrue(u.isValidDomain(uq + "c-testdomain.org"));
        assertTrue(u.isValidDomain("a." + uq + "a-testdomain.org"));
        assertTrue(u.isValidDomain("*." + uq + "a-testdomain.org"));
        assertFalse(u.isValidDomain("a" + uq + "a-testdomain.org"));
        assertFalse(u.isValidDomain("b" + uq + "a-testdomain.org"));

        assertTrue(u.isValidEmail(uq + "a@email.org"));
        assertTrue(u.isValidEmail(uq + "b@email.org"));
        assertFalse(u.isValidEmail(uq + "b+6@email.org"));
        assertFalse(u.isValidEmail(uq + "b*@email.org"));

        assertTrue(u.isValidName("aä b"));
        assertFalse(u.isValidName("aä c"));
        assertFalse(u.isValidName("aä d b"));

    }

    @Test
    public void testDoubleInsert() {
        User u = new User();
        u.setFname("f");
        u.setLname("l");
        u.setMname("m");
        u.setSuffix("s");
        u.setEmail(createUniqueName() + "@example.org");
        u.setDob(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365));
        u.setPreferredLocale(Locale.ENGLISH);
        u.insert(TEST_PASSWORD);
        try {
            u.insert(TEST_PASSWORD);
            fail("Error expected");
        } catch (Error e) {
            // expected
        }
        Assurance[] ma = u.getMadeAssurances();
        Assurance[] ma2 = u.getMadeAssurances();
        Assurance[] ra = u.getReceivedAssurances();
        Assurance[] ra2 = u.getReceivedAssurances();
        assertEquals(0, u.getCertificates(false).length);
        assertEquals(0, ma.length);
        assertEquals(0, ma2.length);
        assertEquals(0, ra.length);
        assertEquals(0, ra2.length);
        assertSame(ma, ma2);
        assertSame(ra, ra2);
    }

    @Test
    public void testGetByMail() {
        String email = createUniqueName() + "a@email.org";
        int id = createVerifiedUser("aä", "b", email, TEST_PASSWORD);
        User emailUser = User.getByEmail(email);
        User u = User.getById(id);
        assertSame(u, emailUser);
    }

}
