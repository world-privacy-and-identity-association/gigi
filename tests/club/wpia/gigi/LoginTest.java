package club.wpia.gigi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.junit.Test;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.ManagedTest;

public class LoginTest extends ManagedTest {

    @Test
    public void testLoginUnverified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        registerUser("an", "bn", email, TEST_PASSWORD);
        getMailReceiver().receive(email);
        assertFalse(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginVerified() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);
        assertTrue(isLoggedin(login(email, TEST_PASSWORD)));
    }

    @Test
    public void testLoginRedirectBack() throws IOException {
        String email = createUniqueName() + "@testmail.org";
        createVerifiedUser("an", "bn", email, TEST_PASSWORD);

        URL u0 = new URL("https://" + getServerName() + SECURE_REFERENCE);
        HttpURLConnection huc0 = (HttpURLConnection) u0.openConnection();
        String headerField = stripCookie(huc0.getHeaderField("Set-Cookie"));

        HttpURLConnection huc = post(headerField, "/login", "username=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8"), 0);

        headerField = huc.getHeaderField("Set-Cookie");
        assertNotNull(headerField);
        assertEquals(302, huc.getResponseCode());
        assertEquals("https://" + getServerName() + SECURE_REFERENCE, huc.getHeaderField("Location"));
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
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "hans"), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        await(c.issue(null, "2y", u));
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        String cookie = login(pk, ce);
        URL u2 = new URL("https://" + getSecureServerName() + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u2.openConnection();
        huc.addRequestProperty("Cookie", cookie);
        authenticateClientCert(pk, ce, huc);
        assertEquals(200, huc.getResponseCode());
    }
}
