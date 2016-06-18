package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Locale;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.mail.MailOverview;
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class TestMailManagement extends ClientTest {

    private String path = MailOverview.DEFAULT_PATH;

    public TestMailManagement() throws IOException {
        clearCaches(); // and reset rate limits
        cookie = login(u.getEmail(), TEST_PASSWORD);
        assertTrue(isLoggedin(cookie));
    }

    @Test
    public void testMailAddInternal() throws InterruptedException, GigiApiException {
        createVerifiedEmail(u);
    }

    @Test
    public void testMailAddInternalFaulty() throws GigiApiException {
        try {
            new EmailAddress(u, "kurti ", Locale.ENGLISH);
            fail();
        } catch (IllegalArgumentException e) {
            // Intended.
        }
    }

    @Test
    public void testMailAddWeb() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String newMail = createUniqueName() + "uni@example.org";
        assertNull(addMail(newMail));
        assertTrue(existsEmail(newMail));
    }

    @Test
    public void testMailAddWebFaulty() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String newMail = createUniqueName() + "uniexample.org";
        assertNotNull(addMail(newMail));
        assertFalse(existsEmail(newMail));
    }

    @Test
    public void testMailAddWebMultiple() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String u = createUniqueName();
        String newMail = u + "uni@eXample.org";
        assertNull(addMail(newMail));
        assertTrue(existsEmail(newMail.toLowerCase()));

        String newMail2 = u + "uni@eXamPlE.org";
        assertNotNull(addMail(newMail2));
        assertTrue(existsEmail(newMail2.toLowerCase()));

        String newMail3 = u + "-buni@eXamPlE.org";
        assertNull(addMail(newMail3));
        assertTrue(existsEmail(newMail.toLowerCase()));
        assertTrue(existsEmail(newMail3.toLowerCase()));
    }

    private String addMail(String newMail) throws IOException, MalformedURLException, UnsupportedEncodingException {
        return executeBasicWebInteraction(cookie, path, "addmail&newemail=" + URLEncoder.encode(newMail, "UTF-8"), 1);
    }

    private boolean existsEmail(String newMail) {
        EmailAddress[] addrs = u.getEmails();
        for (int i = 0; i < addrs.length; i++) {
            if (addrs[i].getAddress().equals(newMail)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testMailSetDefaultWeb() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        EmailAddress addr = createVerifiedEmail(u);
        assertNull(executeBasicWebInteraction(cookie, path, "default=" + addr.getId()));
        ObjectCache.clearAllCaches();
        assertEquals(User.getById(u.getId()).getEmail(), addr.getAddress());
    }

    @Test
    public void testMailSetDefaultWebUnverified() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        EmailAddress addr = new EmailAddress(u, createUniqueName() + "test@test.tld", Locale.ENGLISH);
        assertNotNull(executeBasicWebInteraction(cookie, path, "default=" + addr.getId()));
        assertNotEquals(User.getById(u.getId()).getEmail(), addr.getAddress());
        getMailReciever().clearMails();
    }

    @Test
    public void testMailSetDefaultWebInvalidID() throws MalformedURLException, UnsupportedEncodingException, IOException, InterruptedException, GigiApiException {
        User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "uni@example.org", TEST_PASSWORD));
        int id = -1;
        EmailAddress[] emails = u2.getEmails();
        for (int i = 0; i < emails.length; i++) {
            if (emails[i].getAddress().equals(u2.getEmail())) {
                id = emails[i].getId();
            }
        }
        assertNotEquals(id, -1);
        assertNotNull(executeBasicWebInteraction(cookie, path, "default=" + id));
        assertNotEquals(User.getById(u.getId()).getEmail(), u2.getEmail());
        getMailReciever().clearMails();
    }

    @Test
    public void testMailDeleteWeb() throws InterruptedException, GigiApiException, MalformedURLException, UnsupportedEncodingException, IOException {
        EmailAddress addr = createVerifiedEmail(u);
        assertNull(executeBasicWebInteraction(cookie, path, "delete=" + addr.getId(), 0));
        User u = User.getById(this.u.getId());
        EmailAddress[] addresses = u.getEmails();
        for (int i = 0; i < addresses.length; i++) {
            assertNotEquals(addresses[i].getAddress(), addr.getAddress());
        }
    }

    @Test
    public void testMailDeleteWebMulti() throws InterruptedException, GigiApiException, MalformedURLException, UnsupportedEncodingException, IOException {
        EmailAddress[] addr = new EmailAddress[] {
                createVerifiedEmail(u), createVerifiedEmail(u)
        };
        assertNull(executeBasicWebInteraction(cookie, path, "delete=" + addr[0].getId(), 0));
        assertNull(executeBasicWebInteraction(cookie, path, "delete=" + addr[1].getId(), 0));
        User u = User.getById(this.u.getId());
        EmailAddress[] addresses = u.getEmails();
        for (int i = 0; i < addresses.length; i++) {
            assertNotEquals(addresses[i].getAddress(), addr[0].getAddress());
            assertNotEquals(addresses[i].getAddress(), addr[1].getAddress());
        }
    }

    @Test
    public void testMailDeleteWebFaulty() throws MalformedURLException, UnsupportedEncodingException, IOException {
        User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "uni@test.tld", TEST_PASSWORD));
        EmailAddress em = u2.getEmails()[0];
        assertNotNull(executeBasicWebInteraction(cookie, path, "delete=" + em.getId(), 0));
        u2 = User.getById(u2.getId());
        assertNotEquals(u2.getEmails().length, 0);
    }

    @Test
    public void testMailDeleteWebPrimary() throws MalformedURLException, UnsupportedEncodingException, IOException {
        assertNotNull(executeBasicWebInteraction(cookie, path, "delete=" + u.getEmails()[0].getId(), 0));
        assertNotEquals(u.getEmails().length, 0);
    }
}
