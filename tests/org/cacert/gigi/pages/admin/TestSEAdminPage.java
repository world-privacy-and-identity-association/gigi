package org.cacert.gigi.pages.admin;

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
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class TestSEAdminPage extends ClientTest {

    public TestSEAdminPage() throws IOException {
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
        assertEquals("https://" + getServerName() + "/support/user/" + id, uc.getHeaderField("Location"));
    }
}
