package org.cacert.gigi;

import static org.junit.Assert.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Assurance;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestUser extends ManagedTest {

    @Test
    public void testStoreAndLoad() throws SQLException, GigiApiException {
        long dob = System.currentTimeMillis();
        dob -= dob % (1000 * 60 * 60 * 24);
        User u = new User(createUniqueName() + "a@email.org", "password", new Name("user", "last", "", ""), new java.sql.Date(dob), Locale.ENGLISH);
        int id = u.getId();
        User u2 = User.getById(id);
        assertEquals(u.getName(), u2.getName());
        assertEquals(u.getDoB().toString(), u2.getDoB().toString());
        assertEquals(u.getEmail(), u2.getEmail());
    }

    @Test
    public void testWebStoreAndLoad() throws SQLException {
        int id = createVerifiedUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        Name u = User.getById(id).getName();

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
        Name name = u.getName();
        assertEquals("aä", name.getFname());
        assertEquals("b", name.getLname());
        assertEquals("", name.getMname());
    }

    @Test
    public void testMatcherMethods() throws SQLException, GigiApiException {
        String uq = createUniqueName();
        int id = createVerifiedUser("aä", "b", uq + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
        new EmailAddress(u, uq + "b@email.org", Locale.ENGLISH);
        new EmailAddress(u, uq + "c@email.org", Locale.ENGLISH);
        new Domain(u, uq + "a-testdomain.org");
        new Domain(u, uq + "b-testdomain.org");
        new Domain(u, uq + "c-testdomain.org");
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
    public void testDoubleInsert() throws GigiApiException {
        User u = new User(createUniqueName() + "@example.org", TEST_PASSWORD, new Name("f", "k", "m", "s"), new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365), Locale.ENGLISH);
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
