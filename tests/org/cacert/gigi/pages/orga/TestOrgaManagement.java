package org.cacert.gigi.pages.orga;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.Organisation.Affiliation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestOrgaManagement extends ManagedTest {

    public User u = User.getById(createVerifiedUser("testuser", "testname", uniq + "@testdom.com", TEST_PASSWORD));

    public String session;

    public TestOrgaManagement() throws IOException {
        u.grantGroup(u, Group.getByString("orgassurer"));
        clearCaches();
        session = login(uniq + "@testdom.com", TEST_PASSWORD);
    }

    @Test
    public void testAdd() throws IOException {
        executeBasicWebInteraction(session, CreateOrgPage.DEFAULT_PATH, "O=name&contact=&L=K%C3%B6ln&ST=%C3%9C%C3%96%C3%84%C3%9F&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6", 0);
        Organisation[] orgs = Organisation.getOrganisations(0, 30);
        assertEquals(1, orgs.length);
        assertEquals("name", orgs[0].getName());
        assertEquals("Köln", orgs[0].getCity());
        assertEquals("ÜÖÄß", orgs[0].getProvince());

        User u2 = User.getById(createVerifiedUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        executeBasicWebInteraction(session, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&affiliate=y&master=y", 1);
        List<Affiliation> allAdmins = orgs[0].getAllAdmins();
        assertEquals(1, allAdmins.size());
        Affiliation affiliation = allAdmins.get(0);
        assertSame(u2, affiliation.getTarget());
        assertTrue(affiliation.isMaster());

        executeBasicWebInteraction(session, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&affiliate=y", 1);
        allAdmins = orgs[0].getAllAdmins();
        assertEquals(2, allAdmins.size());
        Affiliation affiliation2 = allAdmins.get(0);
        if (affiliation2.getTarget().getId() == u2.getId()) {
            affiliation2 = allAdmins.get(1);
        }
        assertSame(u.getId(), affiliation2.getTarget().getId());
        assertFalse(affiliation2.isMaster());

        executeBasicWebInteraction(session, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&email=&affiliate=y", 1);
        assertEquals(1, orgs[0].getAllAdmins().size());

        executeBasicWebInteraction(session, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&email=&affiliate=y", 1);
        assertEquals(0, orgs[0].getAllAdmins().size());

        executeBasicWebInteraction(session, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "O=name1&contact=&L=K%C3%B6ln&ST=%C3%9C%C3%96%C3%84%C3%9F&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6", 0);
        clearCaches();
        orgs = Organisation.getOrganisations(0, 30);
        assertEquals("name1", orgs[0].getName());
    }
}
