package org.cacert.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class LoginTest extends ManagedTest {

    @Test
    public void testLoginUnverified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        registerUser("an", "bn", email, TEST_PASSWORD);
        getMailReceiver().receive();
        assertFalse(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginVerified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        assertTrue(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginWrongPassword() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        assertFalse(isLoggedin(login(email, TEST_PASSWORD + "b")));
    }

    @Test
    public void testLogoutVerified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        String cookie = login(email, TEST_PASSWORD);
        assertTrue(isLoggedin(cookie));
        logout(cookie);
        assertFalse(isLoggedin(cookie));
    }

    private void logout(String cookie) throws IOException {
        get(cookie, "/logout").getHeaderField("Location");
    }

    @Test
    public void testLoginMethodDisplay() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        String l = login(email, TEST_PASSWORD);
        URLConnection c = get(l, "");
        String readURL = IOUtils.readURL(c);
        assertThat(readURL, containsString("Password"));
    }

    @Test
    public void testLoginCertificate() throws IOException, GeneralSecurityException, GigiApiException, InterruptedException {
        String email = createUniqueName() + "@testmail.org";
        int user = createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=hans");
        User u = User.getById(user);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "hans"), Digest.SHA256, csr, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        await(c.issue(null, "2y", u));
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        String cookie = login(pk, ce);
        URL u2 = new URL("https://" + getServerName().replaceFirst("^www.", "secure.") + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u2.openConnection();
        huc.addRequestProperty("Cookie", cookie);
        authenticateClientCert(pk, ce, huc);
        assertEquals(200, huc.getResponseCode());
    }
}
