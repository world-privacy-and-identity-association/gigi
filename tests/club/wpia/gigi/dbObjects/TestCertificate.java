package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate.AttachmentType;
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

    @Test
    public void testAttachment() throws GeneralSecurityException, IOException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile());
        assertNull(c.getAttachment(AttachmentType.CRT));
        assertEquals(key, c.getAttachment(AttachmentType.CSR));
        try {
            c.addAttachment(AttachmentType.CSR, "different CSR");
            fail("double add attachment must fail");
        } catch (GigiApiException e) {
            // expected
        }
        assertNull(c.getAttachment(AttachmentType.CRT));
        assertEquals(key, c.getAttachment(AttachmentType.CSR));
        try {
            c.addAttachment(AttachmentType.CRT, null);
            fail("attachment must not be null");
        } catch (GigiApiException e) {
            // expected
        }
        assertNull(c.getAttachment(AttachmentType.CRT));
        assertEquals(key, c.getAttachment(AttachmentType.CSR));
        c.addAttachment(AttachmentType.CRT, "b");
        assertEquals(key, c.getAttachment(AttachmentType.CSR));
        assertEquals("b", c.getAttachment(AttachmentType.CRT));
        try {
            c.addAttachment(AttachmentType.CRT, "different CRT");
            fail("double add attachment must fail");
        } catch (GigiApiException e) {
            // expected
        }
        assertEquals(key, c.getAttachment(AttachmentType.CSR));
        assertEquals("b", c.getAttachment(AttachmentType.CRT));
    }
}
