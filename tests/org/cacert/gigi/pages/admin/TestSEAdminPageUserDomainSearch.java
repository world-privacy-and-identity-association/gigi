package org.cacert.gigi.pages.admin;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.admin.support.FindUserByDomainPage;
import org.cacert.gigi.pages.admin.support.SupportEnterTicketPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestSEAdminPageUserDomainSearch extends ClientTest {

    private Domain d;

    private String domainName;

    private String unique;

    private int tid;

    public TestSEAdminPageUserDomainSearch() throws IOException, GigiApiException {
        grant(email, Group.SUPPORTER);
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

        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + tid, uc.getHeaderField("Location"));
    }

    @Test
    public void testDomainSearchById() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=#" + d.getId());
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + tid, uc.getHeaderField("Location"));
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
        boolean found = false;
        try {
            while (Domain.getById(id) != null && count < 20) {
                count++;
                id = (int) (Math.random() * 10000);
            }
        } catch (Exception e) {
            found = true;
        }
        assumeTrue(found);
        URLConnection uc = post(FindUserByDomainPage.PATH, "process&domain=#" + id);
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }
}
