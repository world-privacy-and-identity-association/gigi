package org.cacert.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.admin.support.FindUserByEmailPage;
import org.cacert.gigi.pages.admin.support.SupportEnterTicketPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestSEAdminPageUserMailSearch extends ClientTest {

    public TestSEAdminPageUserMailSearch() throws IOException {
        grant(email, Group.SUPPORTER);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
    }

    @Test
    public void testFulltextMailSearch() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.com";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode(mail, "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchSingle() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.tld";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example.tld", "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchMultiple() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@example.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);
        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2));
    }

    @Test
    public void testWildcardMailSearchSingleChar() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@fxample.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@_xample.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2));
    }

    @Test
    public void testWildcardMailSearchNoRes() throws MalformedURLException, UnsupportedEncodingException, IOException {
        URLConnection uc = post(FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@_humpfelkumpf.org", "UTF-8"));
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }

    @Test
    public void testFulltextMailSearchNoRes() throws MalformedURLException, UnsupportedEncodingException, IOException {
        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode(createUniqueName() + "@example.org", "UTF-8"), 0);

        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }
}
