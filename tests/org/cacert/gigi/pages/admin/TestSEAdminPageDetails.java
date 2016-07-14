package org.cacert.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.History;
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
        URLConnection uc = get(SupportUserDetailsPage.PATH + id);
        uc.setDoOutput(true);
        String res = IOUtils.readURL(uc);
        assertThat(res, containsString(fname));
        assertThat(res, containsString(lname));
        assertThat(res, containsString(email));
    }

    @Test
    public void testUserDetailsEmail() throws MalformedURLException, IOException, GigiApiException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
        String email2 = createUniqueName() + "@example.com";
        EmailAddress ea = new EmailAddress(User.getById(id), email2, Locale.ENGLISH);
        getMailReceiver().receive().verify();
        // Refresh email Object
        ObjectCache.clearAllCaches();
        ea = EmailAddress.getById(ea.getId());
        assertTrue(ea.isVerified());

        String res = IOUtils.readURL(get(SupportUserDetailsPage.PATH + id));
        assertEquals(2, countRegex(res, Pattern.quote(email)));
        assertEquals(1, countRegex(res, Pattern.quote(email2)));

        User.getById(id).updateDefaultEmail(ea);
        clearCaches();
        res = IOUtils.readURL(get(SupportUserDetailsPage.PATH + id));
        assertEquals(1, countRegex(res, Pattern.quote(email)));
        assertEquals(2, countRegex(res, Pattern.quote(email2)));
    }

    @Test
    public void testUserDetailsEditToLog() throws MalformedURLException, IOException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
        String clientCookie = login(email, TEST_PASSWORD);

        assertEquals(0, logCountAdmin(id));
        assertEquals(0, logCountUser(clientCookie));
        // chaniging both leads to 2 entries
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "dobd=1&dobm=2&doby=2000&detailupdate", 0));
        assertEquals(1, logCountAdmin(id));
        assertEquals(1, logCountUser(clientCookie));

        // Sending same data keeps same
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "dobd=1&dobm=2&doby=2000&detailupdate", 0));
        assertEquals(1, logCountAdmin(id));
        assertEquals(1, logCountUser(clientCookie));

        // changing one leads to one entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "dobd=1&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(2, logCountAdmin(id));
        assertEquals(2, logCountUser(clientCookie));

        // changing one leads to one entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "dobd=2&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(3, logCountAdmin(id));
        assertEquals(3, logCountUser(clientCookie));

        // changing none -> no entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id, "dobd=2&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(3, logCountAdmin(id));
        assertEquals(3, logCountUser(clientCookie));

    }

    private int logCountAdmin(int id) throws IOException {
        return getLogEntryCount(IOUtils.readURL(get(History.SUPPORT_PATH.replace("*", Integer.toString(id)))));
    }

    private int logCountUser(String cookie) throws IOException {
        return getLogEntryCount(IOUtils.readURL(get(cookie, History.PATH)));
    }

    private int getLogEntryCount(String readURL) {
        String s = "<tr><th>Support actions";
        int start = readURL.indexOf(s);
        int end = readURL.indexOf("</table>", start);
        String logs = readURL.substring(start + s.length(), end);
        int i = 0;
        int c = -1;
        while (i != -1) {
            i = logs.indexOf("<tr>", i + 1);
            c++;
        }
        return c;
    }

    private String getFname(String res) {
        Pattern p = Pattern.compile("<span class='fname'>([^<]*)</span>");
        Matcher m = p.matcher(res);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

}
