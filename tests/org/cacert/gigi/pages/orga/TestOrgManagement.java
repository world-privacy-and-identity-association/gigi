package org.cacert.gigi.pages.orga;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Country;
import org.cacert.gigi.dbObjects.Country.CountryCodeType;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.Organisation.Affiliation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.OrgTest;
import org.junit.After;
import org.junit.Test;

public class TestOrgManagement extends OrgTest {

    public TestOrgManagement() throws IOException, GigiApiException {

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
        assertNull(executeBasicWebInteraction(cookie, CreateOrgPage.DEFAULT_PATH, "action=new&O=name&contact=mail@serv.tld&L=K%C3%B6ln&ST=" + URLEncoder.encode(DIFFICULT_CHARS, "UTF-8") + "&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6&optionalName=opname&postalAddress=postaladdress", 0));
        Organisation[] orgs = Organisation.getOrganisations(0, 30);
        assertEquals(1, orgs.length);
        assertEquals("mail@serv.tld", orgs[0].getContactEmail());
        assertEquals("name", orgs[0].getName());
        assertEquals("Köln", orgs[0].getCity());
        assertEquals(DIFFICULT_CHARS, orgs[0].getProvince());
        assertEquals("opname", orgs[0].getOptionalName());
        assertEquals("postaladdress", orgs[0].getPostalAddress());

        User u2 = User.getById(createAssuranceUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&do_affiliate=y&master=y", 1));
        List<Affiliation> allAdmins = orgs[0].getAllAdmins();
        assertEquals(1, allAdmins.size());
        Affiliation affiliation = allAdmins.get(0);
        assertSame(u2, affiliation.getTarget());
        assertTrue(affiliation.isMaster());

        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "email=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&do_affiliate=y", 1));
        allAdmins = orgs[0].getAllAdmins();
        assertEquals(2, allAdmins.size());
        Affiliation affiliation2 = allAdmins.get(0);
        if (affiliation2.getTarget().getId() == u2.getId()) {
            affiliation2 = allAdmins.get(1);
        }
        assertEquals(u.getId(), affiliation2.getTarget().getId());
        assertFalse(affiliation2.isMaster());

        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u.getEmail(), "UTF-8") + "&email=&do_affiliate=y", 1));
        assertEquals(1, orgs[0].getAllAdmins().size());

        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "del=" + URLEncoder.encode(u2.getEmail(), "UTF-8") + "&email=&do_affiliate=y", 1));
        assertEquals(0, orgs[0].getAllAdmins().size());

        assertNull(executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + orgs[0].getId(), "action=updateCertificateData&O=name1&contact=&L=K%C3%B6ln&ST=%C3%9C%C3%96%C3%84%C3%9F&C=DE&comments=jkl%C3%B6loiuzfdfgjlh%C3%B6", 0));
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
            // must fail because u4 is no RA-Agent
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
            // must fail because u4 is no RA-Agent
            o1.addAdmin(u4_dummy, u, false);
            fail("No exception!");
        } catch (GigiApiException e) {
        }
        o1.removeAdmin(u2, u3);
        o1.removeAdmin(u3, u3);
        assertEquals(0, o1.getAllAdmins().size());
        o1.delete();
    }

    @Test
    public void testUpdateOrgCertData() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        o1.updateCertData("name", Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), DIFFICULT_CHARS, "Köln");
        assertEquals("name", o1.getName());
        assertEquals("DE", o1.getState().getCode());
        assertEquals(DIFFICULT_CHARS, o1.getProvince());
        assertEquals("Köln", o1.getCity());
        o1.delete();
    }

    @Test
    public void testUpdateOrgData() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        o1.updateOrgData("mail", "opname", "Köln" + DIFFICULT_CHARS);
        assertEquals("mail", o1.getContactEmail());
        assertEquals("opname", o1.getOptionalName());
        assertEquals("Köln" + DIFFICULT_CHARS, o1.getPostalAddress());
        o1.delete();
    }

    /**
     * Tests various contraints on organisation fields.
     */
    @Test
    public void testLengthConstraint() throws IOException, GigiApiException {
        Organisation o1 = createUniqueOrg();
        String str128 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_";
        String se = "";
        String s64 = str128.substring(0, 64);
        String s65 = str128.substring(0, 65);

        String s128 = str128;
        String s129 = str128 + "a";

        assertNull(upCertData(o1, o1.getName(), null, o1.getProvince(), o1.getCity()));

        // test organisation name
        assertNotNull(upCertData(o1, "", null, o1.getProvince(), o1.getCity()));
        assertNull(upCertData(o1, "A", null, o1.getProvince(), o1.getCity()));
        assertNull(upCertData(o1, s64, null, o1.getProvince(), o1.getCity()));
        assertNotNull(upCertData(o1, s65, null, o1.getProvince(), o1.getCity()));

        // test state
        assertNotNull(upCertData(o1, o1.getName(), null, se, o1.getCity()));
        assertNull(upCertData(o1, o1.getName(), null, "A", o1.getCity()));
        assertNull(upCertData(o1, o1.getName(), null, s128, o1.getCity()));
        assertNotNull(upCertData(o1, o1.getName(), null, s129, o1.getCity()));

        // test town
        assertNotNull(upCertData(o1, o1.getName(), null, o1.getProvince(), se));
        assertNull(upCertData(o1, o1.getName(), null, o1.getProvince(), "A"));
        assertNull(upCertData(o1, o1.getName(), null, o1.getProvince(), s128));
        assertNotNull(upCertData(o1, o1.getName(), null, o1.getProvince(), s129));

        // test country
        assertNotNull(upCertData(o1, o1.getName(), "", o1.getProvince(), o1.getCity()));
        assertNotNull(upCertData(o1, o1.getName(), "D", o1.getProvince(), o1.getCity()));
        assertNull(upCertData(o1, o1.getName(), "DE", o1.getProvince(), o1.getCity()));
        assertNotNull(upCertData(o1, o1.getName(), "DES", o1.getProvince(), o1.getCity()));
        // country code does not exist
        assertNotNull(upCertData(o1, o1.getName(), "DD", o1.getProvince(), o1.getCity()));
        // 3-letter country code should not be accepted
        assertNotNull(upCertData(o1, o1.getName(), "DEU", o1.getProvince(), o1.getCity()));

        // test contact mail
        assertNull(upOptData(o1, o1.getContactEmail()));
        assertNotNull(upOptData(o1, "_mail@domail"));

    }

    /**
     * Updates Organisation optional data via web interface.
     * 
     * @param o1
     *            Organisation to update.
     * @param email
     *            the new contact email
     * @return an error message or <code>null</code>
     */
    private String upOptData(Organisation o1, String email) throws IOException, MalformedURLException, UnsupportedEncodingException {
        return executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + o1.getId(), "action=updateOrganisationData&contact=" + email + "&optionalName=" + o1.getOptionalName() + "&postalAddress=" + o1.getPostalAddress(), 0);
    }

    /**
     * Updates Organisation certificate data via web interface.
     * 
     * @param o1
     *            Organisation to update.
     * @param o
     *            the new name
     * @param c
     *            the new country or <code>null</code> to keep the current
     *            country.
     * @param province
     *            the new "province/state"
     * @param ct
     *            the new city or "locality"
     * @return an error message or <code>null</code>
     */
    private String upCertData(Organisation o1, String o, String c, String province, String ct) throws IOException, MalformedURLException, UnsupportedEncodingException {
        if (c == null) {
            c = o1.getState().getCode();
        }
        return executeBasicWebInteraction(cookie, ViewOrgPage.DEFAULT_PATH + "/" + o1.getId(), "action=updateCertificateData&O=" + o + "&C=" + c + "&ST=" + province + "&L=" + ct, 0);
    }

}
