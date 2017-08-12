package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.dbObjects.Certificate.SubjectAlternateName;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.certs.Certificates;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.ManagedTest;
import sun.security.x509.GeneralNameInterface;

public class TestCertificate extends ManagedTest {

    User u = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));

    @Test
    public void testClientCertLoginStates() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key1, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        await(c.issue(null, "2y", u));
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        assertNotNull(login(pk, ce));
    }

    @Test
    public void testSANs() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile(),//
                new SubjectAlternateName(SANType.EMAIL, "testmail@example.com"), new SubjectAlternateName(SANType.DNS, "testmail.example.com"));

        testFails(CertificateStatus.DRAFT, c);
        await(c.issue(null, "2y", u));
        X509Certificate cert = c.cert();
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();
        assertEquals(2, sans.size());
        boolean hadDNS = false;
        boolean hadEmail = false;
        for (List<?> list : sans) {
            assertEquals(2, list.size());
            Integer type = (Integer) list.get(0);
            switch (type) {
            case GeneralNameInterface.NAME_RFC822:
                hadEmail = true;
                assertEquals("testmail@example.com", list.get(1));
                break;
            case GeneralNameInterface.NAME_DNS:
                hadDNS = true;
                assertEquals("testmail.example.com", list.get(1));
                break;
            default:
                fail("Unknown type");

            }
        }
        assertTrue(hadDNS);
        assertTrue(hadEmail);

        testFails(CertificateStatus.ISSUED, c);

        Certificate c2 = Certificate.getBySerial(c.getSerial());
        assertNotNull(c2);
        assertEquals(2, c2.getSANs().size());
        assertEquals(c.getSANs().get(0).getName(), c2.getSANs().get(0).getName());
        assertEquals(c.getSANs().get(0).getType(), c2.getSANs().get(0).getType());
        assertEquals(c.getSANs().get(1).getName(), c2.getSANs().get(1).getName());
        assertEquals(c.getSANs().get(1).getType(), c2.getSANs().get(1).getType());

        try {
            c2.getSANs().remove(0);
            fail("the list should not be modifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testCertCreateSHA384() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA384, key, CSRType.CSR, getClientProfile());
        await(c.issue(null, "2y", u));
        assertThat(c.cert().getSigAlgName().toLowerCase(), CoreMatchers.containsString("sha384"));
    }

    @Test
    public void testCertLifeCycle() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();

        testFails(CertificateStatus.DRAFT, c);
        await(c.issue(null, "2y", u));

        String cookie = login(u.getEmail(), TEST_PASSWORD);
        testFails(CertificateStatus.ISSUED, c);
        X509Certificate cert = c.cert();
        c.setLoginEnabled(true);
        assertNotNull(login(pk, cert));
        assertEquals(1, countRegex(IOUtils.readURL(get(cookie, Certificates.PATH)), "<td>(?:REVOKED|ISSUED)</td>"));
        assertEquals(1, countRegex(IOUtils.readURL(get(cookie, Certificates.PATH + "?withRevoked")), "<td>(?:REVOKED|ISSUED)</td>"));
        await(c.revoke(RevocationType.USER));

        testFails(CertificateStatus.REVOKED, c);
        assertNull(login(pk, cert));

        assertEquals(0, countRegex(IOUtils.readURL(get(cookie, Certificates.PATH)), "<td>(?:REVOKED|ISSUED)</td>"));
        assertEquals(1, countRegex(IOUtils.readURL(get(cookie, Certificates.PATH + "?withRevoked")), "<td>(?:REVOKED|ISSUED)</td>"));
    }

    private void testFails(CertificateStatus status, Certificate c) throws IOException, GeneralSecurityException, SQLException, GigiApiException {
        assertEquals(status, c.getStatus());
        if (status != CertificateStatus.ISSUED) {
            try {
                c.revoke(RevocationType.USER);
                fail(status + " is in invalid state");
            } catch (IllegalStateException ise) {

            }
        }
        if (status != CertificateStatus.DRAFT) {
            try {
                c.issue(null, "2y", u);
                fail(status + " is in invalid state");
            } catch (IllegalStateException ise) {

            }
        }
        if (status != CertificateStatus.ISSUED) {
            try {
                c.cert();
                if (status != CertificateStatus.REVOKED) {
                    fail(status + " is in invalid state");
                }
            } catch (IllegalStateException ise) {

            }
        }
    }
}
