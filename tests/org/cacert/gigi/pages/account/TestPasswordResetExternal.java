package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.pages.wot.TestAssurance;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.cacert.gigi.util.RandomToken;
import org.junit.Test;

public class TestPasswordResetExternal extends ClientTest {

    @Test
    public void testByAssurance() throws IOException {
        User u = User.getById(createAssuranceUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));
        String cookie2 = login(u.getEmail(), TEST_PASSWORD);
        URLConnection uc = TestAssurance.buildupAssureFormConnection(cookie2, email, true);
        String avalue = RandomToken.generateToken(32);
        uc.getOutputStream().write(("assuredName=" + this.u.getPreferredName().getId() + "&date=" + TestAssurance.validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10&passwordReset=1&passwordResetValue=" + URLEncoder.encode(avalue, "UTF-8")).getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        assertNull(error);

        TestMail mail = getMailReceiver().receive();
        assertThat(mail.getSubject(), containsString("Verification"));
        mail = getMailReceiver().receive();
        assertEquals(mail.getTo(), this.u.getEmail());
        String link = mail.extractLink();
        String npw = TEST_PASSWORD + "'";
        System.out.println(link);
        assertNotNull(toPasswordReset(avalue, link, npw, npw + "'"));
        assertNotNull(toPasswordReset(avalue + "'", link, npw, npw));
        assertNotNull(toPasswordReset(avalue, link, "a", "a"));
        assertNull(toPasswordReset(avalue, link, npw, npw));
        assertNotNull(login(email, npw));
    }

    private String toPasswordReset(String avalue, String link, String npw, String npw2) throws IOException, MalformedURLException, UnsupportedEncodingException {
        URLConnection uc2 = new URL(link).openConnection();
        String csrf = getCSRF(uc2);
        String headerField = uc2.getHeaderField("Set-Cookie");
        assertNotNull(headerField);
        String cookie3 = stripCookie(headerField);
        uc2 = new URL("https://" + getServerName() + PasswordResetPage.PATH).openConnection();
        cookie(uc2, cookie3);
        uc2.setDoOutput(true);
        OutputStream o = uc2.getOutputStream();
        o.write(("csrf=" + csrf + "&pword1=" + URLEncoder.encode(npw, "UTF-8") + "&pword2=" + URLEncoder.encode(npw2, "UTF-8") + "&private_token=" + URLEncoder.encode(avalue, "UTF-8")).getBytes("UTF-8"));
        System.out.println(((HttpURLConnection) uc2).getResponseCode());
        String readURL = IOUtils.readURL(uc2);
        return fetchStartErrorMessage(readURL);
    }
}
