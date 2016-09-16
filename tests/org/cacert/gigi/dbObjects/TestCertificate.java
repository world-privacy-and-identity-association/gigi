package org.cacert.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.testUtils.ClientBusinessTest;
import org.junit.Test;

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
