package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Date;
import java.util.Calendar;
import java.util.TimeZone;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestMyDetailsEdit extends ManagedTest {

    String email = createUniqueName() + "@e.de";

    int id = createVerifiedUser("Kurti", "Hansel", email, TEST_PASSWORD);

    String cookie = login(email, TEST_PASSWORD);

    public TestMyDetailsEdit() throws IOException {}

    @Test
    public void testChangeFnameValid() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "fname=" + newName + "&lname=Hansel&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals(newName, u.getName().getFname());
    }

    @Test
    public void testChangeLnameValid() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "lname=" + newName + "&fname=Kurti&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals(newName, u.getName().getLname());
    }

    @Test
    public void testChangeMnameValid() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "mname=" + newName + "&fname=Kurti&lname=Hansel&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals(newName, u.getName().getMname());
    }

    @Test
    public void testChangeSuffixValid() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "mname=&fname=Kurti&lname=Hansel&suffix=" + newName + "&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals(newName, u.getName().getSuffix());
    }

    @Test
    public void testUnsetSuffix() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "mname=&fname=Kurti&lname=Hansel&suffix=" + newName + "&day=1&month=1&year=2000&processDetails", 0));
        clearCaches();
        User u = User.getById(id);
        assertEquals(newName, u.getName().getSuffix());
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "mname=&fname=Kurti&lname=Hansel&suffix=&day=1&month=1&year=2000&processDetails", 0));
        clearCaches();
        u = User.getById(id);
        assertEquals("", u.getName().getSuffix());
    }

    @Test
    public void testUnsetFname() throws IOException {
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "fname=&lname=Hansel&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals("", u.getName().getFname());

    }

    @Test
    public void testUnsetLname() throws IOException {
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "lname=&fname=Kurti&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals("Hansel", u.getName().getLname());
    }

    @Test
    public void testUnsetMname() throws IOException {
        String newName = createUniqueName();
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "mname=" + newName + "&fname=Kurti&lname=Hansel&suffix=&day=1&month=1&year=2000&processDetails", 0));
        User u = User.getById(id);
        assertEquals(newName, u.getName().getMname());
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "lname=Hansel&fname=Kurti&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        clearCaches();
        u = User.getById(id);
        assertEquals("", u.getName().getMname());

    }

    @Test
    public void testChangeDOBValid() throws IOException {
        assertNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "lname=Hansel&fname=Kurti&mname=&suffix=&day=1&month=2&year=2000&processDetails", 0));
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
        assertNotNull(executeBasicWebInteraction(cookie, MyDetails.PATH, "lname=Hansel&fname=Kurti&mname=&suffix=&day=1&month=1&year=test&processDetails", 0));
    }
}
