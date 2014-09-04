package org.cacert.gigi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import sun.security.x509.GeneralNameInterface;
import static org.junit.Assert.*;

public class TestCertificate extends ManagedTest {

    @Test
    public void testClientCertLoginStates() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key1, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y").waitFor(60000);
        final X509Certificate ce = c.cert();
        assertNotNull(login(pk, ce));
    }

    @Test
    public void testSANs() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key, CSRType.CSR, CertificateProfile.getById(1),//
                new SubjectAlternateName(SANType.EMAIL, "testmail@example.com"), new SubjectAlternateName(SANType.DNS, "testmail.example.com"));

        testFails(CertificateStatus.DRAFT, c);
        c.issue(null, "2y").waitFor(60000);
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
    public void testCertLifeCycle() throws IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key, CSRType.CSR, CertificateProfile.getById(1));
        final PrivateKey pk = kp.getPrivate();

        testFails(CertificateStatus.DRAFT, c);
        c.issue(null, "2y").waitFor(60000);

        testFails(CertificateStatus.ISSUED, c);
        X509Certificate cert = c.cert();
        assertNotNull(login(pk, cert));
        c.revoke().waitFor(60000);

        testFails(CertificateStatus.REVOKED, c);
        assertNull(login(pk, cert));

    }

    private void testFails(CertificateStatus status, Certificate c) throws IOException, GeneralSecurityException, SQLException, GigiApiException {
        assertEquals(status, c.getStatus());
        if (status != CertificateStatus.ISSUED) {
            try {
                c.revoke();
                fail(status + " is in invalid state");
            } catch (IllegalStateException ise) {

            }
        }
        if (status != CertificateStatus.DRAFT) {
            try {
                c.issue(null, "2y");
                fail(status + " is in invalid state");
            } catch (IllegalStateException ise) {

            }
        }
        if (status != CertificateStatus.ISSUED) {
            try {
                c.cert();
                fail(status + " is in invalid state");
            } catch (IllegalStateException ise) {

            }
        }
    }
}
