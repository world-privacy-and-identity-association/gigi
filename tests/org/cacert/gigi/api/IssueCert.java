package org.cacert.gigi.api;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

import sun.security.x509.X500Name;

public class IssueCert extends ClientTest {

    @Test
    public void testIssueCert() throws Exception {
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "EMAIL=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("EMAIL", "testmail@example.com"), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y", u).waitFor(60000);
        final X509Certificate ce = c.cert();
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + "/account/certs/new").openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(("profile=client&csr=" + URLEncoder.encode(generatePEMCSR(kp, "EMAIL=" + email + ",CN=CAcert WoT User"), "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals(connection.getResponseCode(), 200);
        String cert = IOUtils.readURL(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.Certificate xcert = cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals("CAcert WoT User", ((X500Name) ((X509Certificate) xcert).getSubjectDN()).getCommonName());
    }
}
