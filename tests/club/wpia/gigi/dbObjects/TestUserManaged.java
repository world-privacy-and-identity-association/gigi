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

public class TestUserManaged extends ManagedTest {

    @Test
    public void testDeleteEmailWithCertificate() throws GigiApiException, GeneralSecurityException, IOException, InterruptedException {

        int id = createVerifiedUser("Test", "User", createUniqueName() + "test@test.tld", TEST_PASSWORD);
        String email = createUniqueName() + "test@test.tld";
        User u = User.getById(id);
        Certificate[] certs = u.getCertificates(false);
        int certCount = certs.length;
        EmailAddress testAddress = createVerifiedEmail(u, email);
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=" + email);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", email), Digest.SHA256, key, CSRType.CSR, getClientProfile(), new Certificate.SubjectAlternateName(SANType.EMAIL, email));
        c.issue(null, "2y", u).waitFor(60000);

        u.deleteEmail(testAddress);

        assertFalse(c.getRevocationDate().toString().isEmpty());
        certs = u.getCertificates(false);
        assertEquals(certCount, certs.length);

    }
}
