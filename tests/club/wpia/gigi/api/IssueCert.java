package club.wpia.gigi.api;

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

import org.junit.Test;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
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
            c = new Certificate(u, u, Certificate.buildDN("EMAIL", "testmail@example.com"), Digest.SHA256, key1, CSRType.CSR, getClientProfile());
            c.setLoginEnabled(true);
            pk = kp.getPrivate();
            await(c.issue(null, "2y", u));
            ce = c.cert();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Test
    public void testIssueCert() throws Exception {
        String cert = issueCert(generatePEMCSR(kp, "EMAIL=" + email + ",CN=" + CertificateRequest.DEFAULT_CN), "profile=client");

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.X509Certificate xcert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals(CertificateRequest.DEFAULT_CN, ((X500Name) xcert.getSubjectDN()).getCommonName());

    }

    @Test
    public void testRevoke() throws Exception {
        revoke(c.getSerial().toLowerCase());
        assertEquals(CertificateStatus.REVOKED, c.getStatus());
    }

    @Test
    public void testIssueCertVerified() throws Exception {
        makeAgent(id);

        String intendedName = "a b";
        String cert = issueCert(generatePEMCSR(kp, "EMAIL=" + email + ",CN=" + intendedName), "profile=client-a");

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        java.security.cert.X509Certificate xcert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getBytes("UTF-8")));
        assertEquals(intendedName, ((X500Name) xcert.getSubjectDN()).getCommonName());

    }

    @Test
    public void testIssueOrgCert() throws Exception {
        makeAgent(id);
        User u2 = User.getById(createVerificationUser("testworker", "testname", createUniqueName() + "@testdom.com", TEST_PASSWORD));
        u2.grantGroup(getSupporter(), Group.ORG_AGENT);

        Organisation o1 = new Organisation("name", Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "pr", "st", "test@mail", "", "", u2);
        o1.addAdmin(u, u2, false);
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
