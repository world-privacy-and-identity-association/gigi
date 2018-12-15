package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
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

    @Test
    public void testActor() throws GeneralSecurityException, IOException, GigiApiException {
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile());

        assertEquals(u, c.getActor());
        assertEquals("AB", c.getActor().getInitials());
    }

    @Test
    public void testFingerprint() throws IOException, GeneralSecurityException, GigiApiException {
        Certificate c = importCertificate();
        assertEquals("fa6175b369627d47a52b9fd73e87ccf087afbd10", c.getFingerprint("sha-1"));
        assertEquals("98c3f2a5424d2404e0b2ccdae17d8cbc949ea36bddb0c3f152a931f88c17c3d3", c.getFingerprint("sha-256"));
    }

    private Certificate importCertificate() throws GigiApiException {
        int certID;
        try (GigiPreparedStatement inserter = new GigiPreparedStatement("INSERT INTO certs SET md=?::`mdType`, csr_type=?::`csrType`, memid=?, profile=?, actorid=?, created=NOW(), caid=?, expire=NOW()")) {
            inserter.setString(1, Digest.SHA512.toString().toLowerCase());
            inserter.setString(2, CSRType.CSR.toString());
            inserter.setInt(3, u.getId());
            inserter.setInt(4, 10);
            inserter.setInt(5, u.getId());
            inserter.setInt(6, 4);
            inserter.execute();
            certID = inserter.lastInsertId();
        }

        try (GigiPreparedStatement insertAVA = new GigiPreparedStatement("INSERT INTO `certAvas` SET `certId`=?, name=?, value=?")) {
            insertAVA.setInt(1, certID);
            insertAVA.setString(2, "EMAIL");
            insertAVA.setString(3, u.getEmail());
            insertAVA.execute();
        }
        Certificate c = Certificate.getById(certID);
        String pem = "-----BEGIN CERTIFICATE-----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-----END CERTIFICATE-----";
        c.addAttachment(AttachmentType.CRT, pem);
        return c;
    }
}
