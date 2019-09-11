package club.wpia.gigi.pages.admin;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Random;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.admin.support.FindCertPage;
import club.wpia.gigi.pages.admin.support.FindUserByDomainPage;
import club.wpia.gigi.pages.admin.support.FindUserByEmailPage;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketForm;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestSEAdminTicketSetting extends ClientTest {

    public TestSEAdminTicketSetting() throws IOException, GigiApiException {
        grant(u, Group.SUPPORTER);
        try {
            KeyPair kp = generateKeypair();
            String csr = generatePEMCSR(kp, "CN=" + u.getPreferredName().toString());
            Certificate c = new Certificate(u, u, Certificate.buildDN("CN", u.getPreferredName().toString()), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
            final PrivateKey pk = kp.getPrivate();
            await(c.issue(null, "2y", u));
            final X509Certificate ce = c.cert();
            c.setLoginEnabled(true);
            cookie = login(pk, ce);
            loginCertificate = c;
            loginPrivateKey = pk;
        } catch (InterruptedException e) {
            throw new GigiApiException(e.toString());
        } catch (GeneralSecurityException e) {
            throw new GigiApiException(e.toString());
        }
    }

    @Test
    public void testFulltextMailSearch() throws MalformedURLException, UnsupportedEncodingException, IOException {
        assertEquals(403, get(FindUserByEmailPage.PATH).getResponseCode());
        assertEquals(302, post(SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
        assertEquals(200, get(FindUserByEmailPage.PATH).getResponseCode());
        assertEquals(200, get(FindUserByDomainPage.PATH).getResponseCode());
        assertEquals(302, post(SupportEnterTicketPage.PATH, "ticketno=a20140808.8&deleteTicket=action", 0).getResponseCode());
        assertEquals(403, get(FindUserByEmailPage.PATH).getResponseCode());
    }

    @Test
    public void testSetTicketNumberCharacter() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String ticket;
        String alphabet = "abcdefghijklmnopqrstuvwxyz";

        // test allowed character
        for (char ch : SupportEnterTicketForm.TICKET_PREFIX.toCharArray()) {
            ticket = ch + "20171212.1";
            assertEquals(302, post(SupportEnterTicketPage.PATH, "ticketno=" + ticket + "&setTicket=action", 0).getResponseCode());
            ticket = Character.toUpperCase(ch) + "20171212.1";
            assertEquals(302, post(SupportEnterTicketPage.PATH, "ticketno=" + ticket + "&setTicket=action", 0).getResponseCode());
            alphabet = alphabet.replaceAll(Character.toString(ch), "");
        }

        // test not allowed character
        Random rnd = new Random();
        char ch = alphabet.charAt(rnd.nextInt(alphabet.length()));
        assertWrongTicketNumber(ch + "20171212.1");
    }

    @Test
    public void testSetTicketNumberDatepart() throws MalformedURLException, UnsupportedEncodingException, IOException {
        char ch = getValidCharacter();

        assertWrongTicketNumber(ch + "220171212.1");

        assertWrongTicketNumber(ch + "0171212.1");

        assertWrongTicketNumber(ch + "20171512.1");

        assertWrongTicketNumber(ch + "20170229.1");

        assertWrongTicketNumber(ch + ch + "20171212.1");

        assertWrongTicketNumber("20171212.1");

        assertWrongTicketNumber(ch + "20171212" + ch + ".1");

        assertWrongTicketNumber(ch + "201721" + ch + "21.1");
    }

    @Test
    public void testSetTicketNumberNumberpart() throws MalformedURLException, UnsupportedEncodingException, IOException {
        char ch = getValidCharacter();

        assertWrongTicketNumber(ch + "20171212.");

        assertWrongTicketNumber(ch + "20171212");

        assertWrongTicketNumber(ch + "20171212.1" + ch);

    }

    private char getValidCharacter() {
        Random rnd = new Random();
        return SupportEnterTicketForm.TICKET_PREFIX.charAt(rnd.nextInt(SupportEnterTicketForm.TICKET_PREFIX.length()));
    }

    private void assertWrongTicketNumber(String ticket) throws IOException {
        String res = IOUtils.readURL(post(SupportEnterTicketPage.PATH, "ticketno=" + ticket + "&setTicket=action"));
        assertThat(res, containsString("Ticket format malformed"));
    }

    @Test
    public void testPWLogin() throws MalformedURLException, UnsupportedEncodingException, IOException {
        String cookiePW = login(email, TEST_PASSWORD);
        loginCertificate = null;
        assertEquals(403, get(cookiePW, SupportEnterTicketPage.PATH).getResponseCode());
        assertEquals(403, get(cookiePW, FindUserByEmailPage.PATH).getResponseCode());
        assertEquals(403, get(cookiePW, FindUserByDomainPage.PATH).getResponseCode());
        assertEquals(403, get(cookiePW, FindCertPage.PATH).getResponseCode());
    }

}
