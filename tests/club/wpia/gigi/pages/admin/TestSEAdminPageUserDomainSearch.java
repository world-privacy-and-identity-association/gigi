package club.wpia.gigi.pages.admin;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.admin.support.FindUserByDomainPage;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestSEAdminPageUserDomainSearch extends ClientTest {

    private Domain d;

    private String domainName;

    private String unique;

    private int tid;

    public TestSEAdminPageUserDomainSearch() throws IOException, GigiApiException {
        grant(u, Group.SUPPORTER);
        cookie = login(email, TEST_PASSWORD);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());

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
}
