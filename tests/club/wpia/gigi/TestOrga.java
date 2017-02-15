package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.BusinessTest;

public class TestOrga extends BusinessTest {

    @Test
    public void testAddRm() throws GigiApiException, IOException {
        User u1 = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        u1.grantGroup(getSupporter(), Group.ORGASSURER);
        User u2 = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        u2.grantGroup(getSupporter(), Group.ORGASSURER);
        User u3 = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        u3.grantGroup(getSupporter(), Group.ORGASSURER);
        User u4 = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@email.org", TEST_PASSWORD));
        u4.grantGroup(getSupporter(), Group.ORGASSURER);
        Organisation o1 = new Organisation("name", Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "prov", "city", "email", "optional name", "postal address", u1);
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
