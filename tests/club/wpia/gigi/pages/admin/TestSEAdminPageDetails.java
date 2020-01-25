package club.wpia.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.History;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.SEClientTest;
import club.wpia.gigi.util.DayDate;

public class TestSEAdminPageDetails extends SEClientTest {

    private Certificate cs;

    public TestSEAdminPageDetails() throws IOException, GigiApiException {
        cs = loginCertificate;
    }

    @Test
    public void testUserDetailsDisplay() throws MalformedURLException, IOException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
        URLConnection uc = get(SupportUserDetailsPage.PATH + id + "/");
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
        getMailReceiver().receive(email2).verify();
        // Refresh email Object
        ObjectCache.clearAllCaches();
        ea = EmailAddress.getById(ea.getId());
        assertTrue(ea.isVerified());

        String res = IOUtils.readURL(get(SupportUserDetailsPage.PATH + id + "/"));
        assertEquals(2, countRegex(res, Pattern.quote(email)));
        assertEquals(1, countRegex(res, Pattern.quote(email2)));

        User.getById(id).updateDefaultEmail(ea);
        clearCaches();
        res = IOUtils.readURL(get(SupportUserDetailsPage.PATH + id + "/"));
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
        // changing both leads to 2 entries
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id + "/", "dobd=1&dobm=2&doby=2000&detailupdate", 0));
        assertEquals(1, logCountAdmin(id));
        assertEquals(1, logCountUser(clientCookie));

        // Sending same data keeps same
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id + "/", "dobd=1&dobm=2&doby=2000&detailupdate", 0));
        assertEquals(1, logCountAdmin(id));
        assertEquals(1, logCountUser(clientCookie));

        // changing one leads to one entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id + "/", "dobd=1&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(2, logCountAdmin(id));
        assertEquals(2, logCountUser(clientCookie));

        // changing one leads to one entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id + "/", "dobd=2&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(3, logCountAdmin(id));
        assertEquals(3, logCountUser(clientCookie));

        // changing none -> no entry
        assertNull(executeBasicWebInteraction(cookie, SupportUserDetailsPage.PATH + id + "/", "dobd=2&dobm=3&doby=2000&detailupdate", 0));
        assertEquals(3, logCountAdmin(id));
        assertEquals(3, logCountUser(clientCookie));

        // discarding all 6 mails
        for (int i = 0; i < 6; i++) {
            getMailReceiver().receive(null);
        }
    }

    @Test
    public void testUserDetailsMyPoints() throws MalformedURLException, IOException {
        String email = createUniqueName() + "@example.com";
        String fname = "Först";
        String lname = "Secönd";
        int id = createVerifiedUser(fname, lname, email, TEST_PASSWORD);
        String clientCookie = login(email, TEST_PASSWORD);

        // try to open mypoints as user
        loginCertificate = null;
        HttpURLConnection uc = get(clientCookie, SupportUserDetailsPage.PATH + id + "/points");

        assertEquals(403, uc.getResponseCode());

        // enter verification and open mypoints as supporter
        loginCertificate = cs;
        makeAgent(this.id);
        String location = createUniqueName();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, this.id);
            ps.setInt(2, User.getById(id).getPreferredName().getId());
            ps.setInt(3, 10);
            ps.setString(4, location);
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 200));
            ps.execute();
        }

        uc = get(cookie, SupportUserDetailsPage.PATH + id + "/points");

        String res = IOUtils.readURL(uc);
        assertThat(res, containsString("Support User Points"));
        assertThat(res, containsString(location));

        // remove ticket number and try to access mypoints from supporter
        // account
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "deleteTicket=action", 0).getResponseCode());

        uc = get(cookie, SupportUserDetailsPage.PATH + id + "/points");

        assertEquals(403, uc.getResponseCode());

    }

    private int logCountAdmin(int id) throws IOException {
        return getLogEntryCount(IOUtils.readURL(get(History.SUPPORT_PATH.replace("*", Integer.toString(id)))));
    }

    private int logCountUser(String cookie) throws IOException {
        loginCertificate = null;
        int count = getLogEntryCount(IOUtils.readURL(get(cookie, History.PATH)));
        loginCertificate = cs;
        return count;
    }

    private int getLogEntryCount(String readURL) {
        String s = "<tr><th>Actions in user account";
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

    @Test
    public void testUserDetailsRAAgent() throws IOException, GigiApiException {
        User u0 = User.getById(createVerifiedUser("Kurti", "Hansel", createUniqueName() + "@email.com", TEST_PASSWORD));

        HttpURLConnection uc = get(cookie, SupportUserDetailsPage.PATH + u0.getId() + "/");
        String res = IOUtils.readURL(uc);
        assertThat(res, containsString("No (RA Agent Contract signed: No)"));

        signRAContract(u0);
        uc = get(cookie, SupportUserDetailsPage.PATH + u0.getId() + "/");
        res = IOUtils.readURL(uc);
        assertThat(res, containsString("No (RA Agent Contract signed: Yes)"));

        insertPassedTest(u0.getId());
        uc = get(cookie, SupportUserDetailsPage.PATH + u0.getId() + "/");
        res = IOUtils.readURL(uc);
        assertThat(res, containsString("No (RA Agent Contract signed: Yes)"));

        insertVerificationPoints(u0.getId());
        uc = get(cookie, SupportUserDetailsPage.PATH + u0.getId() + "/");
        res = IOUtils.readURL(uc);
        assertThat(res, not(containsString("RA Agent Contract signed:")));

    }
}
