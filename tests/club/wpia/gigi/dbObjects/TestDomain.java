package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.testUtils.ManagedTest;

public class TestDomain extends ManagedTest {

    @Test
    public void testDeleteDomainWithCertificate() throws GigiApiException, GeneralSecurityException, IOException, InterruptedException {
        User u = User.getById(createVerificationUser("Kurti", "Hansel", createUniqueName() + "@email.com", TEST_PASSWORD));
        String domain = createUniqueName() + ".org";
        Domain d = new Domain(u, u, domain);
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=" + domain);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", domain), Digest.SHA256, key, CSRType.CSR, getClientProfile(), new Certificate.SubjectAlternateName(SANType.DNS, domain));
        c.issue(null, "2y", u).waitFor(60000);

        c = new Certificate(u, u, Certificate.buildDN("CN", domain), Digest.SHA256, key, CSRType.CSR, getClientProfile(), new Certificate.SubjectAlternateName(SANType.DNS, "www." + domain));
        c.issue(null, "2y", u).waitFor(60000);

        Certificate[] certs = d.fetchActiveCertificates();
        assertEquals(2, certs.length);

        d.delete();

        certs = u.getCertificates(false);
        assertEquals(0, certs.length);
        certs = d.fetchActiveCertificates();
        assertEquals(0, certs.length);

    }

}
