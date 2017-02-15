package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestCertificate extends ClientBusinessTest {

    @Test
    public void testSetLoginEnabled() throws GeneralSecurityException, IOException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile());

        assertFalse(c.isLoginEnabled());
        c.setLoginEnabled(true);
        assertTrue(c.isLoginEnabled());
        c.setLoginEnabled(true);
        assertTrue(c.isLoginEnabled());
        c.setLoginEnabled(false);
        assertFalse(c.isLoginEnabled());
        c.setLoginEnabled(false);
        assertFalse(c.isLoginEnabled());
    }
}
