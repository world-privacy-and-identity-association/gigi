package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

import org.cacert.gigi.dbObjects.Assurance;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.Country;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.NamePart;
import org.cacert.gigi.dbObjects.NamePart.NamePartType;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.BusinessTest;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class TestUser extends BusinessTest {

    @Test
    public void testStoreAndLoad() throws SQLException, GigiApiException {
        long dob = System.currentTimeMillis();
        dob -= dob % (1000 * 60 * 60 * 24);
        User u = createUser("f", "l", createUniqueName() + "a@email.org", TEST_PASSWORD);
        int id = u.getId();
        User u2 = User.getById(id);
        assertEquals(u.getNames()[0], u2.getNames()[0]);
        assertEquals(u.getDoB().toString(), u2.getDoB().toString());
        assertEquals(u.getEmail(), u2.getEmail());
    }

    @Test
    public void testWebStoreAndLoad() throws SQLException, GigiApiException {
        int id = createVerifiedUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        Name u = User.getById(id).getNames()[0];

        assertThat(Arrays.asList(u.getParts()), CoreMatchers.hasItem(new NamePart(NamePartType.FIRST_NAME, "aä")));
        assertThat(Arrays.asList(u.getParts()), CoreMatchers.hasItem(new NamePart(NamePartType.LAST_NAME, "b")));
        assertEquals(2, u.getParts().length);
    }

    @Test
    public void testAssurerUtilMethods() throws SQLException, GigiApiException {
        int id = createAssuranceUser("aä", "b", createUniqueName() + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
        assertTrue(u.canAssure());
        int assurancePoints = u.getAssurancePoints();
        int expPoints = u.getExperiencePoints();
        assertEquals(100, assurancePoints);
        assertEquals(User.EXPERIENCE_POINTS, expPoints);
        assertTrue(u.hasPassedCATS());
        assertEquals(10, u.getMaxAssurePoints());
    }

    @Test
    public void testMatcherMethodsDomain() throws SQLException, GigiApiException, IOException {
        String uq = createUniqueName();
        int id = createVerifiedUser("aä", "b", uq + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);
        verify(new Domain(u, u, uq + "a-testdomain.org"));
        verify(new Domain(u, u, uq + "b-testdomain.org"));
        verify(new Domain(u, u, uq + "c-testdomain.org"));
        assertEquals(3, u.getDomains().length);
        assertTrue(u.isValidDomain(uq + "a-testdomain.org"));
        assertTrue(u.isValidDomain(uq + "b-testdomain.org"));
        assertTrue(u.isValidDomain(uq + "c-testdomain.org"));
        assertTrue(u.isValidDomain("a." + uq + "a-testdomain.org"));
        assertTrue(u.isValidDomain("*." + uq + "a-testdomain.org"));
        assertFalse(u.isValidDomain("a" + uq + "a-testdomain.org"));
        assertFalse(u.isValidDomain("b" + uq + "a-testdomain.org"));
    }

    @Test
    public void testMatcherMethodsEmail() throws SQLException, GigiApiException, IOException {
        String uq = createUniqueName();
        int id = createVerifiedUser("aä", "b", uq + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);

        new EmailAddress(u, uq + "b@email.org", Locale.ENGLISH);
        getMailReceiver().receive().verify();
        new EmailAddress(u, uq + "c@email.org", Locale.ENGLISH);
        getMailReceiver().receive();// no-verify
        assertEquals(3, u.getEmails().length);

        assertTrue(u.isValidEmail(uq + "a@email.org"));
        assertTrue(u.isValidEmail(uq + "b@email.org"));
        assertFalse(u.isValidEmail(uq + "b+6@email.org"));
        assertFalse(u.isValidEmail(uq + "b*@email.org"));
        assertFalse(u.isValidEmail(uq + "c@email.org"));
    }

    @Test
    public void testMatcherMethodsName() throws SQLException, GigiApiException, IOException {
        String uq = createUniqueName();
        int id = createVerifiedUser("aä", "b", uq + "a@email.org", TEST_PASSWORD);

        User u = User.getById(id);

        User[] us = new User[5];
        for (int i = 0; i < us.length; i++) {
            us[i] = User.getById(createAssuranceUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
            Notary.assure(us[i], u, u.getPreferredName(), u.getDoB(), 10, "here", validVerificationDateString(), AssuranceType.FACE_TO_FACE, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS));
        }

        assertTrue(u.isValidName("aä b"));
        assertFalse(u.isValidName("aä c"));
        assertFalse(u.isValidName("aä d b"));

    }

    @Test
    public void testDoubleInsert() throws GigiApiException {
        long d = System.currentTimeMillis();
        d -= d % DayDate.MILLI_DAY;
        User u = createUser("f", "l", createUniqueName() + "@example.org", TEST_PASSWORD);
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
    public void testGetByMail() throws GigiApiException {
        String email = createUniqueName() + "a@email.org";
        int id = createVerifiedUser("aä", "b", email, TEST_PASSWORD);
        User emailUser = User.getByEmail(email);
        User u = User.getById(id);
        assertSame(u, emailUser);
    }

    @Test
    public void testNoCats() throws GigiApiException {
        String email = createUniqueName() + "a@email.org";
        createVerifiedUser("aä", "b", email, TEST_PASSWORD);
        User emailUser = User.getByEmail(email);
        assertFalse(emailUser.hasPassedCATS());
    }

    @Test
    public void testGetByMailFail() {
        String email = createUniqueName() + "d@email.org";
        User emailUser = User.getByEmail(email);
        assertNull(emailUser);
    }

}
