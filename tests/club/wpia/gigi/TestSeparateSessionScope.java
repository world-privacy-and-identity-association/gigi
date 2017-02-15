package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import org.junit.Test;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.ManagedTest;

public class TestSeparateSessionScope extends ManagedTest {

    @Test
    public void testSeparateScope() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        String mail = "thisgo" + createUniqueName() + "@example.com";
        int user = createAssuranceUser("test", "tugo", mail, TEST_PASSWORD);
        String cookie = login(mail, TEST_PASSWORD);
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=hans");
        User u = User.getById(user);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "hans"), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        await(c.issue(null, "2y", u));
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        String scookie = login(pk, ce);

        assertTrue(isLoggedin(cookie));
        assertFalse(isLoggedin(scookie));

        checkCertLogin(c, pk, scookie, 200);
        checkCertLogin(c, pk, cookie, 302);
    }

    @Test
    public void testSerialSteal() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        String mail = "thisgo" + createUniqueName() + "@example.com";
        int user = createAssuranceUser("test", "tugo", mail, TEST_PASSWORD);
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=hans");
        User u = User.getById(user);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "hans"), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
        Certificate c2 = new Certificate(u, u, Certificate.buildDN("CN", "hans"), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        Job j1 = c.issue(null, "2y", u);
        await(c2.issue(null, "2y", u));
        await(j1);
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        String scookie = login(pk, ce);

        checkCertLogin(c, pk, scookie, 200);
        checkCertLogin(c2, pk, scookie, 403);
        checkCertLogin(c, pk, scookie, 302);

    }

    private void checkCertLogin(Certificate c2, final PrivateKey pk, String scookie, int expected) throws IOException, NoSuchAlgorithmException, KeyManagementException, GeneralSecurityException {
        URL u = new URL("https://" + getSecureServerName() + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        authenticateClientCert(pk, c2.cert(), huc);
        huc.setRequestProperty("Cookie", scookie);
        assertEquals(expected, huc.getResponseCode());
    }
}
