package club.wpia.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.admin.support.FindUserByDomainPage;
import club.wpia.gigi.pages.admin.support.SupportOrgDomainPage;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.SEClientTest;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestSEAdminPageUserDomainSearch extends SEClientTest {

    private Domain d;

    private String domainName;

    private String unique;

    private int tid;

    public TestSEAdminPageUserDomainSearch() throws IOException, GigiApiException {
        String mail = createUniqueName() + "@example.com";
        tid = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User user = User.getById(tid);
        unique = createUniqueName();
        domainName = unique + "pattern.org";
        this.d = new Domain(user, user, domainName);
    }

    @Test
    public void testDomainSearch() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=" + URLEncoder.encode(domainName, "UTF-8"));

        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + tid + "/", uc.getHeaderField("Location"));
    }

    @Test
    public void testDomainSearchById() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=#" + d.getId());
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + tid + "/", uc.getHeaderField("Location"));
    }

    @Test
    public void testDomainSearchNonExist() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=" + URLEncoder.encode(createUniqueName() + ".de", "UTF-8"));
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }

    @Test
    public void testDomainSearchByIdNonExist() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        int id = (int) (Math.random() * 10000);
        int count = 0;
        while (Domain.getById(id) != null && count < 20) {
            count++;
            id = (int) (Math.random() * 10000);
        }
        Assume.assumeThat(Domain.getById(id), CoreMatchers.nullValue());
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=#" + id);
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }

    @Test
    public void testOrgDomainSearch() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        // generate organisation with domain
        u.grantGroup(getSupporter(), Group.ORG_AGENT);
        Organisation o1 = new Organisation(createUniqueName(), Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "city", "test@example.com", "", "", u);
        String dom = createUniqueName() + ".de";
        Domain d = new Domain(u, o1, dom);

        // test
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=" + URLEncoder.encode(dom, "UTF-8"));

        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportOrgDomainPage.PATH + d.getId(), uc.getHeaderField("Location"));

        String s = IOUtils.readURL(get(cookie, SupportOrgDomainPage.PATH + d.getId()));
        assertThat(s, containsString(dom));
        assertThat(s, containsString(o1.getName()));

        // test malformated id
        HttpURLConnection uc1 = get(SupportOrgDomainPage.PATH + d.getId() + "a");
        assertEquals(400, uc1.getResponseCode());

        // test non existing id
        uc1 = get(SupportOrgDomainPage.PATH + "5000");
        assertEquals(400, uc1.getResponseCode());

    }

    @Test
    public void testDomainSearchByMalformatedId() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=#" + d.getId() + "a");
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }

}
