package org.cacert.gigi.pages.admin;

import static org.cacert.gigi.testUtils.ManagedTest.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.admin.support.SupportEnterTicketPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

public class TestSEAdminPageDetails extends ClientTest {

    public TestSEAdminPageDetails() throws IOException {
        grant(email, Group.SUPPORTER);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
    }

    @Test
    public void testUserDetailsDisplay() throws MalformedURLException, IOException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
        URLConnection uc = new URL("https://" + getServerName() + SupportUserDetailsPage.PATH + id).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        String res = IOUtils.readURL(uc);
        assertThat(res, containsString("<input type=\"text\" value=\"" + fname + "\" name=\"fname\">"));
        assertThat(res, containsString("<input type=\"text\" value=\"" + lname + "\" name=\"lname\">"));
        assertThat(res, containsString(email));
    }
}
