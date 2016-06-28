package org.cacert.gigi.pages.orga;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.Organisation.Affiliation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.OrgTest;
import org.junit.After;
import org.junit.Test;

public class TestOrgManagement extends OrgTest {

    public TestOrgManagement() throws IOException {

    }

    @After
    public void purgeDbAfterTest() throws SQLException, IOException {
        purgeDatabase();
    }

    @Test
    public void testAdd() throws IOException {
        for (Organisation i : Organisation.getOrganisations(0, 30)) {
            i.delete();
        }
        executeBasicWebInteraction(cookie, CreateOrgPage.DEFAULT_PATH, "action=new&O=name&contact=mail&L=K%C3%B6ln&ST=%C3%9C%C3%96%C3%84%C3%9F&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6", 0);
        Organisation[] orgs = Organisation.getOrganisations(0, 30);
        assertEquals(1, orgs.length);
        assertEquals("mail", orgs[0].getContactEmail());
        assertEquals("name", orgs[0].getName());
        assertEquals("Köln", orgs[0].getCity());
        assertEquals("ÜÖÄß", orgs[0].getProvince());

        User u2 = User.getById(createAssuranceUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1);
        List<Affiliation> allAdmins = orgs[0].getAllAdmins();
        assertEquals(1, allAdmins.size());
        Affiliation affiliation = allAdmins.get(0);
        assertSame(u2, affiliation.getTarget());
        assertTrue(affiliation.isMaster());

        executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&do_affiliate=y", 1);
        allAdmins = orgs[0].getAllAdmins();
        assertEquals(2, allAdmins.size());
        Affiliation affiliation2 = allAdmins.get(0);
        if (affiliation2.getTarget().getId() == u2.getId()) {
            affiliation2 = allAdmins.get(1);
        }
        assertEquals(u.getId(), affiliation2.getTarget().getId());
        assertFalse(affiliation2.isMaster());

        executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&email=&do_affiliate=y", 1);
        assertEquals(1, orgs[0].getAllAdmins().size());

        executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&email=&do_affiliate=y", 1);
        assertEquals(0, orgs[0].getAllAdmins().size());

        executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "action=updateCertificateData&O=name1&contact=&L=K%C3%B6ln&ST=%C3%9C%C3%96%C3%84%C3%9F&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6", 0);
        clearCaches();
        orgs = Organisation.getOrganisations(0, 30);
        assertEquals("name1", orgs[0].getName());
    }

    @Test
    public void testNonAssurerSeeOnlyOwn() throws IOException, GigiApiException {
        User u2 = User.getById(createAssuranceUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        Organisation o1 = createUniqueOrg();
        Organisation o2 = createUniqueOrg();
        o1.addAdmin(u2, u, false);
        String session2 = login(u2.getEmail(), TEST_PASSWORD);

        URLConnection uc = get(session2, ViewOrgPage.DEFAULT_PATH);
        assertEquals(403, ((HttpURLConnection) uc).getResponseCode());

        uc = get(session2, MyDetails.PATH);
        String content = IOUtils.readURL(uc);
        assertThat(content, containsString(o1.getName()));
        assertThat(content, not(containsString(o2.getName())));
        uc = get(session2, ViewOrgPage.DEFAULT_PATH + "/" + o1.getId());
        assertEquals(403, ((HttpURLConnection) uc).getResponseCode());
        uc = get(session2, ViewOrgPage.DEFAULT_PATH + "/" + o2.getId());
        assertEquals(403, ((HttpURLConnection) uc).getResponseCode());

        uc = get(ViewOrgPage.DEFAULT_PATH);
        content = IOUtils.readURL(uc);
        assertThat(content, containsString(o1.getName()));
        assertThat(content, containsString(o2.getName()));
        uc = get(ViewOrgPage.DEFAULT_PATH + "/" + o1.getId());
        assertEquals(200, ((HttpURLConnection) uc).getResponseCode());
        uc = get(ViewOrgPage.DEFAULT_PATH + "/" + o2.getId());
        assertEquals(200, ((HttpURLConnection) uc).getResponseCode());
        o1.delete();
        o2.delete();
    }

    @Test
    public void testAffiliationRights() throws IOException, GigiApiException {
        User u2 = User.getById(createAssuranceUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        User u3 = User.getById(createAssuranceUser("testmaster", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        User u4_dummy = User.getById(createVerifiedUser("testmaster", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        Organisation o1 = createUniqueOrg();
        o1.addAdmin(u3, u, true);
        try {
            // must fail because u4 is no assurer
            o1.addAdmin(u4_dummy, u3, false);
            fail("No exception!");
        } catch (GigiApiException e) {
        }
        o1.addAdmin(u2, u3, false);
        try {
            // must fail because u2 may not add admins
            o1.addAdmin(u3, u2, false);
            fail("No exception!");
        } catch (GigiApiException e) {
        }
        try {
            // must fail because u4 is no assurer
            o1.addAdmin(u4_dummy, u, false);
            fail("No exception!");
        } catch (GigiApiException e) {
        }
        o1.removeAdmin(u2, u3);
        o1.removeAdmin(u3, u3);
        assertEquals(0, o1.getAllAdmins().size());
        o1.delete();
    }
}
