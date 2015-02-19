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
import java.util.Collection;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

import sun.security.x509.X500Name;

public class IssueCert extends ClientTest {

    @Test
    public void testIssueCert() throws Exception {
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, Certificate.buildDN("CN", "testmail@example.com"), "sha256", key1, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y").waitFor(60000);
        final X509Certificate ce = c.cert();
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + "/account/certs/new").openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(("csr=" + URLEncoder.encode(generatePEMCSR(kp, "CN=a b"), "UTF-8")).getBytes());
        os.flush();
        assertEquals(connection.getResponseCode(), 200);
        String cert = IOUtils.readURL(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Collection<? extends java.security.cert.Certificate> certs = cf.generateCertificates(new ByteArrayInputStream(cert.getBytes()));
        assertEquals("a b", ((X500Name) ((X509Certificate) certs.iterator().next()).getSubjectDN()).getCommonName());
    }
}