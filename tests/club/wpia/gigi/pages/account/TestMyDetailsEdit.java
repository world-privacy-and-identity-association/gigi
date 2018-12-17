package club.wpia.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.ManagedTest;

public class TestMyDetailsEdit extends ManagedTest {

    String email = createUniqueName() + "@e.de";

    int id = createVerifiedUser("Kurti", "Hansel", email, TEST_PASSWORD);

    String cookie = login(email, TEST_PASSWORD);

    public TestMyDetailsEdit() throws IOException {}

    @Test
    public void testAddName() throws IOException {
        int startn = User.getById(id).getNames().length;
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "name-type=western&fname=" + newName + "&lname=Hansel&action=addName", 0));
        User u = User.getById(id);

        NamePart[] parts = u.getNames()[startn].getParts();
        assertThat(Arrays.asList(parts), CoreMatchers.hasItem(new NamePart(NamePartType.FIRST_NAME, newName)));
        assertThat(Arrays.asList(parts), CoreMatchers.hasItem(new NamePart(NamePartType.LAST_NAME, "Hansel")));
        assertEquals(2, parts.length);
        assertEquals(startn + 1, User.getById(id).getNames().length);
    }

    @Test
    public void testDelName() throws IOException, GigiApiException {
        User user = User.getById(id);
        int startn = user.getNames().length;
        String newName = createUniqueName();
        Name n1 = new Name(user, new NamePart(NamePartType.SINGLE_NAME, newName));

        assertEquals(startn + 1, user.getNames().length);
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "removeName=" + n1.getId(), 0));
        assertEquals(startn, user.getNames().length);
    }

    @Test
    public void testDelDefaultName() throws IOException {
        User user = User.getById(id);
        assertEquals(1, user.getNames().length);
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "removeName=" + user.getNames()[0].getId(), 0));
        assertEquals(1, user.getNames().length);
    }

    @Test
    public void testChangeDOBValid() throws IOException {
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "day=1&month=2&year=2000&action=updateDoB", 0));
        User u = User.getById(id);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, 2000);
        cal.set(Calendar.DAY_OF_MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.MONTH, 1);
        Date d = new Date(cal.getTimeInMillis());
        assertEquals(d.toString(), u.getDoB().toSQLDate().toString());
    }

    @Test
    public void testChangeDOBInvalid() throws IOException {
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "day=1&month=1&year=test&action=updateDoB", 0));
    }

    /**
     * Tests that changing the date of birth to a too recent one results in an
     * error.
     * 
     * @throws IOException
     *             when web interactions fail.
     * @see club.wpia.gigi.TestCalendarUtil#testIsOfAge()
     */
    @Test
    public void testChangeTooYoung() throws IOException {
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.YEAR, -User.MINIMUM_AGE);
        c.add(Calendar.DAY_OF_MONTH, +2);
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "day=" + c.get(Calendar.DAY_OF_MONTH) + "&month=" + (c.get(Calendar.MONTH) + 1) + "&year=" + c.get(Calendar.YEAR) + "&action=updateDoB", 0));
    }

    @Test
    public void testChangeTooOld() throws IOException {
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.YEAR, -User.MAXIMUM_PLAUSIBLE_AGE);
        c.add(Calendar.DAY_OF_MONTH, -2);
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "day=" + c.get(Calendar.DAY_OF_MONTH) + "&month=" + (c.get(Calendar.MONTH) + 1) + "&year=" + c.get(Calendar.YEAR) + "&action=updateDoB", 0));
    }

    @Test
    public void testChangeResidenceCountry() throws IOException {
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "residenceCountry=DE&action=updateResidenceCountry", 0));
        User user = User.getById(id);
        assertEquals("DE", user.getResidenceCountry().getCode());
    }

    @Test
    public void testChangeResidenceCountryToNull() throws IOException {
        User user = User.getById(id);
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "residenceCountry=invalid&action=updateResidenceCountry", 0));
        assertEquals(null, user.getResidenceCountry());
    }

    @Test
    public void testModifyUserGroup() throws IOException {
        User user = User.getById(id);
        // test add group
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "action=addGroup&groupToModify=" + URLEncoder.encode(Group.LOCATE_AGENT.getDBName(), "UTF-8"), 0));

        user = User.getById(id);
        user.refreshGroups();
        assertTrue(user.isInGroup(Group.LOCATE_AGENT));

        // test remove group
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "action=removeGroup&groupToModify=" + URLEncoder.encode(Group.LOCATE_AGENT.getDBName(), "UTF-8"), 0));

        user = User.getById(id);
        user.refreshGroups();
        assertFalse(user.isInGroup(Group.LOCATE_AGENT));

        // test add group that only support can add
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "action=addGroup&groupToModify=" + URLEncoder.encode(Group.SUPPORTER.getDBName(), "UTF-8"), 0));

        user = User.getById(id);
        user.refreshGroups();
        assertFalse(user.isInGroup(Group.SUPPORTER));

        // test add invalid group
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "action=addGroup&groupToModify=non-existing", 0));
    }

    @Test
    public void testHyphen() throws IOException {

        String fn = "Hans-Dieter";
        String fnc = fn;
        String ln = "Müller-Schmitz";
        String lnc = ln;
        testAddName(fn, ln, fnc, lnc);

        fn = "Hans-   Dieter";
        ln = "Müller-    Schmitz";
        testAddName(fn, ln, fnc, lnc);

        fn = "Hans    -Dieter";
        ln = "Müller    -Schmitz";
        testAddName(fn, ln, fnc, lnc);

        fn = "Hans    -     Dieter";
        ln = "Müller   -   Schmitz";
        testAddName(fn, ln, fnc, lnc);

        String[] hyphen = {
                "\u002d", "\u058a", "\u05be", "\u1806", "\u2010", "\u2011", "\u2012", "\u2013", "\u2014", "\u2015", "\u2e3a", "\u2e3b", "\ufe58", "\ufe63", "\uff0d"
        };

        for (int i = 0; i < hyphen.length; i++) {
            fn = "Hans    " + hyphen[i] + "     Dieter";
            ln = "Müller   " + hyphen[i] + "   Schmitz";
            testAddName(fn, ln, fnc, lnc);
        }
    }

    @Test
    public void testBlanks() throws IOException {

        String fn = "Hans";
        String fnc = fn;
        String ln = "Müller";
        String lnc = ln;
        testAddName(fn, ln, fnc, lnc);

        fn = "Hans  ";
        ln = "Müller  ";
        testAddName(fn, ln, fnc, lnc);

        fn = "    Hans";
        ln = "    Müller";
        testAddName(fn, ln, fnc, lnc);

        fn = "Hans    Dieter";
        ln = "Müller    Schmitz";
        testAddName(fn, ln, fnc, lnc, 4);

        fn = "Hans    Dieter   ";
        ln = "    Müller    Schmitz";
        testAddName(fn, ln, fnc, lnc, 4);
    }

    public void testAddName(String fn, String ln, String fnc, String lnc) throws IOException {
        testAddName(fn, ln, fnc, lnc, 2);
    }

    public void testAddName(String fn, String ln, String fnc, String lnc, int partLength) throws IOException {
        int startn = User.getById(id).getNames().length;
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "action=addName&name-type=western&fname=" + fn + "&lname=" + ln, 0));
        User u = User.getById(id);

        NamePart[] parts = u.getNames()[startn].getParts();
        assertThat(Arrays.asList(parts), CoreMatchers.hasItem(new NamePart(NamePartType.FIRST_NAME, fnc)));
        assertThat(Arrays.asList(parts), CoreMatchers.hasItem(new NamePart(NamePartType.LAST_NAME, lnc)));
        assertEquals(partLength, parts.length);
        assertEquals(startn + 1, User.getById(id).getNames().length);
    }
}
