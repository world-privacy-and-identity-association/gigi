package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestSeparateSessionScope extends ManagedTest {

    @Test
    public void testSeparateScope() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        String mail = "thisgo" + createUniqueName() + "@example.com";
        int user = createAssuranceUser("test", "tugo", mail, TEST_PASSWORD);
        String cookie = login(mail, TEST_PASSWORD);
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=felix@dogcraft.de");
        Certificate c = new Certificate(user, "/CN=testmail@example.com", "sha256", csr, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y").waitFor(60000);
        final X509Certificate ce = c.cert();
        String scookie = login(pk, ce);

        assertTrue(isLoggedin(cookie));
        assertFalse(isLoggedin(scookie));

        URL u = new URL("https://" + getServerName().replaceAll("^www", "secure") + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        authenticateClientCert(pk, ce, huc);
        huc.setRequestProperty("Cookie", scookie);
        assertEquals(200, huc.getResponseCode());

        HttpURLConnection huc2 = (HttpURLConnection) u.openConnection();
        authenticateClientCert(pk, ce, huc2);
        huc2.setRequestProperty("Cookie", cookie);
        assertEquals(302, huc2.getResponseCode());

    }
}
