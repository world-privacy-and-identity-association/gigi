package org.cacert.gigi;

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

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.Job;
import org.junit.Test;

public class TestSeparateSessionScope extends ManagedTest {

    @Test
    public void testSeparateScope() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        String mail = "thisgo" + createUniqueName() + "@example.com";
        int user = createAssuranceUser("test", "tugo", mail, TEST_PASSWORD);
        String cookie = login(mail, TEST_PASSWORD);
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=felix@dogcraft.de");
        Certificate c = new Certificate(User.getById(user), Certificate.buildDN("CN", "testmail@example.com"), "sha256", csr, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y").waitFor(60000);
        final X509Certificate ce = c.cert();
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
        String csr = generatePEMCSR(kp, "CN=felix@dogcraft.de");
        Certificate c = new Certificate(User.getById(user), Certificate.buildDN("CN", "testmail@example.com"), "sha256", csr, CSRType.CSR, CertificateProfile.getById(1));
        Certificate c2 = new Certificate(User.getById(user), Certificate.buildDN("CN", "testmail@example.com"), "sha256", csr, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        Job j1 = c.issue(null, "2y");
        c2.issue(null, "2y").waitFor(60000);
        j1.waitFor(60000);
        final X509Certificate ce = c.cert();
        String scookie = login(pk, ce);

        checkCertLogin(c, pk, scookie, 200);
        checkCertLogin(c2, pk, scookie, 403);
        checkCertLogin(c, pk, scookie, 302);

    }

    private void checkCertLogin(Certificate c2, final PrivateKey pk, String scookie, int expected) throws IOException, NoSuchAlgorithmException, KeyManagementException, GeneralSecurityException {
        URL u = new URL("https://" + getServerName().replaceAll("^www", "secure") + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        authenticateClientCert(pk, c2.cert(), huc);
        huc.setRequestProperty("Cookie", scookie);
        assertEquals(expected, huc.getResponseCode());
    }
}
