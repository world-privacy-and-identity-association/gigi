package org.cacert.gigi.api;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

import sun.security.x509.X500Name;

public class IssueCert extends ClientTest {

    private final PrivateKey pk;

    private final X509Certificate ce;

    private final Certificate c;

    private final KeyPair kp;

    public IssueCert() {
        try {
            kp = generateKeypair();
            String key1 = generatePEMCSR(kp, "EMAIL=testmail@example.com");
            c = new Certificate(u, u, Certificate.buildDN("EMAIL", "testmail@example.com"), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getById(1));
            pk = kp.getPrivate();
            c.issue(null, "2y", u).waitFor(60000);
            ce = c.cert();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Test
    public void testIssueCert() throws Exception {
        String cert = issueCert(generatePEMCSR(kp, "EMAIL=" + email + ",CN=CAcert WoT User"), "profile=client");

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.X509Certificate xcert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals("CAcert WoT User", ((X500Name) xcert.getSubjectDN()).getCommonName());

    }

    @Test
    public void testRevoke() throws Exception {
        revoke(c.getSerial().toLowerCase());
        assertEquals(CertificateStatus.REVOKED, c.getStatus());
    }

    @Test
    public void testIssueCertAssured() throws Exception {
        makeAssurer(id);

        Name n = u.getName();
        String whishName = n.getFname() + " " + n.getLname();
        String cert = issueCert(generatePEMCSR(kp, "EMAIL=" + email + ",CN=" + whishName), "profile=client-a");

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.X509Certificate xcert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals(whishName, ((X500Name) xcert.getSubjectDN()).getCommonName());

    }

    @Test
    public void testIssueOrgCert() throws Exception {
        makeAssurer(id);
        u.grantGroup(u, Group.ORGASSURER);

        Organisation o1 = new Organisation("name", "st", "pr", "st", "test@mail", u);
        o1.addAdmin(u, u, false);
        String testdom = createUniqueName() + "-example.com";
        Domain d2 = new Domain(u, o1, testdom);
        verify(d2);

        String whishName = createUniqueName();
        String cert = issueCert(generatePEMCSR(kp, "EMAIL=test@" + testdom + ",CN=" + whishName), "profile=client-orga&asOrg=" + o1.getId());

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.X509Certificate xcert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals(whishName, ((X500Name) xcert.getSubjectDN()).getCommonName());

    }

    private String issueCert(String csr, String options) throws IOException, GeneralSecurityException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + CreateCertificate.PATH).openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write((options + "&csr=" + URLEncoder.encode(csr, "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals(connection.getResponseMessage(), 200, connection.getResponseCode());
        String cert = IOUtils.readURL(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        return cert;
    }

    private void revoke(String serial) throws IOException, GeneralSecurityException {
        HttpURLConnection connection;
        OutputStream os;
        connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + "/account/certs/revoke").openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        os = connection.getOutputStream();
        os.write(("serial=" + URLEncoder.encode(serial, "UTF-8")).getBytes("UTF-8"));
        os.flush();
        assertEquals(connection.getResponseCode(), 200);
    }
}
