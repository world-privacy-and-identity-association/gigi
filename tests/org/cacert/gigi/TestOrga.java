package org.cacert.gigi;

import static org.junit.Assert.*;

import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestOrga extends ManagedTest {

    @Test
    public void testAddRm() throws GigiApiException {
        User u1 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        User u3 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        User u4 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        Organisation o1 = new Organisation("name", "ST", "prov", "city", "email", u1);
        assertEquals(0, o1.getAllAdmins().size());
        o1.addAdmin(u2, u1, false);
        assertEquals(1, o1.getAllAdmins().size());
        o1.addAdmin(u2, u1, false); // Insert double should be ignored
        assertEquals(1, o1.getAllAdmins().size());
        o1.addAdmin(u3, u1, false);
        assertEquals(2, o1.getAllAdmins().size());
        o1.addAdmin(u4, u1, false);
        assertEquals(3, o1.getAllAdmins().size());
        o1.removeAdmin(u3, u1);
        assertEquals(2, o1.getAllAdmins().size());
        o1.addAdmin(u3, u1, false); // add again
        assertEquals(3, o1.getAllAdmins().size());
        o1.removeAdmin(u3, u1);
        assertEquals(2, o1.getAllAdmins().size());
        o1.removeAdmin(u4, u1);
        o1.removeAdmin(u2, u1);
        assertEquals(0, o1.getAllAdmins().size());
    }

}
