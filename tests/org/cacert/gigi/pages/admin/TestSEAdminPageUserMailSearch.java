package org.cacert.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.admin.support.FindUserPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestSEAdminPageUserMailSearch extends ClientTest {

    public TestSEAdminPageUserMailSearch() throws IOException {
        grant(email, Group.SUPPORTER);
    }

    @Test
    public void testFulltextMailSearch() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.com";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode(mail, "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchSingle() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.tld";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode("%@example.tld", "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testWildcardMailSearchMultiple() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@example.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode("%@example.org", "UTF-8")).getBytes("UTF-8"));
        os.flush();
        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2));
    }

    @Test
    public void testWildcardMailSearchSingleChar() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String mail = createUniqueName() + "@example.org";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        String mail2 = createUniqueName() + "@example.org";
        int id2 = createVerifiedUser("Först", "Secönd", mail2, TEST_PASSWORD);
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode("%@_xample.org", "UTF-8")).getBytes("UTF-8"));
        os.flush();
        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id));
        assertThat(res, containsString(SupportUserDetailsPage.PATH + id2));
    }

    @Test
    public void testWildcardMailSearchNoRes() throws MalformedURLException, UnsupportedEncodingException, IOException {
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode("%@_humpfelkumpf.org", "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }

    @Test
    public void testFulltextMailSearchNoRes() throws MalformedURLException, UnsupportedEncodingException, IOException {
        URLConnection uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindUserPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&email=" + URLEncoder.encode(createUniqueName() + "@example.org", "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }
}
