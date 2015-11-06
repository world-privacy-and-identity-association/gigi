package org.cacert.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.account.MyDetails;
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

    @Test
    public void testUserDetailsEdit() throws MalformedURLException, IOException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);

        String userCookie = login(email, TEST_PASSWORD);
        assertEquals("Först", getFname(IOUtils.readURL(get(userCookie, MyDetails.PATH, 0))));
        // User can change his name
        assertNull(executeBasicWebInteraction(userCookie, MyDetails.PATH, "fname=Kurti&lname=Hansel&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        assertEquals("Kurti", getFname(IOUtils.readURL(get(userCookie, MyDetails.PATH, 0))));
        // But when assurer
        makeAssurer(id);
        // User cannot change his name, and the form changed
        assertNotNull(executeBasicWebInteraction(userCookie, MyDetails.PATH, "fname=Kurti2&lname=Hansel&mname=&suffix=&day=1&month=1&year=2000&processDetails", 0));
        assertNull(getFname(IOUtils.readURL(get(userCookie, MyDetails.PATH, 0))));
        assertEquals("Kurti", getFnamePlain(IOUtils.readURL(get(userCookie, MyDetails.PATH, 0))));

        // but support still can
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "fname=Kurti3&lname=Hansel&mname=&suffix=&dobd=1&dobm=2&doby=2000&detailupdate", 0));
        assertEquals("Kurti3", getFnamePlain(IOUtils.readURL(get(userCookie, MyDetails.PATH, 0))));

    }

    private String getFname(String res) {
        Pattern p = Pattern.compile("<input type=\"text\" name=\"fname\" value=\"([^\"]*)\">");
        Matcher m = p.matcher(res);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String getFnamePlain(String res) {
        Pattern p = Pattern.compile("\\s*<td width=\"125\">First Name: </td>\\s*<td width=\"125\">([^<]*)</td>");
        Matcher m = p.matcher(res);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
