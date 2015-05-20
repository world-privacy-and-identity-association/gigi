package org.cacert.gigi.pages.admin;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.admin.support.FindDomainPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestSEAdminPageUserDomainSearch extends ClientTest {

    public TestSEAdminPageUserDomainSearch() throws IOException {
        grant(email, Group.SUPPORTER);
    }

    @Test
    public void testDomainSearch() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        String mail = createUniqueName() + "@example.com";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User user = User.getById(id);
        String domainName = createUniqueName() + ".org";
        Domain d = new Domain(user, domainName);
        d.insert();
        URLConnection uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&domain=" + URLEncoder.encode(domainName, "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testDomainSearchById() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        String mail = createUniqueName() + "@example.com";
        int id = createVerifiedUser("Först", "Secönd", mail, TEST_PASSWORD);
        User user = User.getById(id);
        String domainName = createUniqueName() + ".org";
        Domain d = new Domain(user, domainName);
        d.insert();
        URLConnection uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&domain=#" + d.getId()).getBytes("UTF-8"));
        os.flush();
        assertEquals("https://" + ServerConstants.getWwwHostNamePortSecure() + SupportUserDetailsPage.PATH + id, uc.getHeaderField("Location"));
    }

    @Test
    public void testDomainSearchNonExist() throws MalformedURLException, UnsupportedEncodingException, IOException, GigiApiException {
        URLConnection uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);

        uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&domain=" + URLEncoder.encode(createUniqueName() + ".de", "UTF-8")).getBytes("UTF-8"));
        os.flush();
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
        URLConnection uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc, 0);
        uc = new URL("https://" + getServerName() + FindDomainPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + "process&domain=#" + id).getBytes("UTF-8"));
        os.flush();
        assertNotNull(fetchStartErrorMessage(IOUtils.readURL(uc)));
    }
}
