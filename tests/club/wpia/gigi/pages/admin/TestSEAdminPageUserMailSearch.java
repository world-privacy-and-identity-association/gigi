package club.wpia.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.admin.support.FindUserByEmailPage;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.SEClientTest;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestSEAdminPageUserMailSearch extends SEClientTest {

    public TestSEAdminPageUserMailSearch() throws IOException, GigiApiException {}

    @Test
    public void testFulltextMailSearch() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.com";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode(mail, "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + id + "/", uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchSingle() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.tld";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example.tld", "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + id + "/", uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchMultiple() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@example.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);
        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id + "/"));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2 + "/"));
    }

    @Test
    public void testWildcardMailSearchSingleChar() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@fxample.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@_xample.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id + "/"));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2 + "/"));
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

    @Test
    public void testSearchSecondEmailAddress() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        String mail = createUniqueName() + "@example1.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User testuser = User.getById(id);
        String mail2 = createUniqueName() + "@example1.org";
        createVerifiedEmail(testuser, mail2);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode(mail2, "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + id + "/", uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchSecondEmailAddress() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        clearCaches();
        String mail = createUniqueName() + "@example2.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User testuser = User.getById(id);
        String mail2 = createUniqueName() + "@example2.org";
        createVerifiedEmail(testuser, mail2);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example2.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(mail));
        assertThat(res, containsString(mail2));
    }

    @Test
    public void testWildcardMailSearchMultipleEmailAddressOneAccount() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        clearCaches();
        String mail = createUniqueName() + "@example3.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User testuser = User.getById(id);
        String mail2 = createUniqueName() + "@test3.org";
        createVerifiedEmail(testuser, mail2);
        String mail3 = createUniqueName() + "@test3.org";
        createVerifiedEmail(testuser, mail3);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example3.org", "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + SupportUserDetailsPage.PATH + id + "/", uc.getHeaderField("Location"));

        uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@test3.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, not(containsString(mail)));
        assertThat(res, containsString(mail2));
        assertThat(res, containsString(mail3));
    }

    @Test
    public void testWildcardMailSearchMultipleEmailAddressMultipleAccounts() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        String mail = createUniqueName() + "1@example4.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User testuser = User.getById(id);
        String mail2 = createUniqueName() + "@test4.org";
        createVerifiedEmail(testuser, mail2);

        String mail3 = createUniqueName() + "2@example4.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail3, TEST_PASSWORD);
        User testuser2 = User.getById(id2);
        String mail4 = createUniqueName() + "@test4.org";
        createVerifiedEmail(testuser2, mail4);

        URLConnection uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@example4.org", "UTF-8"), 0);

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(mail));
        assertThat(res, not(containsString(mail2)));
        assertThat(res, containsString(mail3));
        assertThat(res, not(containsString(mail4)));

        uc = post(cookie, FindUserByEmailPage.PATH, "process&email=" + URLEncoder.encode("%@test4.org", "UTF-8"), 0);

        res = IOUtils.readURL(uc);
        assertThat(res, not(containsString(mail)));
        assertThat(res, containsString(mail2));
        assertThat(res, not(containsString(mail3)));
        assertThat(res, containsString(mail4));
    }
}
