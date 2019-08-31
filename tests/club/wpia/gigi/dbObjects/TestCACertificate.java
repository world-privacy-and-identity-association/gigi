package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.junit.Before;
import org.junit.Test;

import club.wpia.gigi.testUtils.ClientBusinessTest;

public class TestCACertificate extends ClientBusinessTest {

    public CertificateFactory fact;

    public CACertificate root, orga;

    public int rootId, orgaId;

    public X509Certificate configRoot;

    @Before
    public void getTestCertificates() throws CertificateException, FileNotFoundException {
        fact = CertificateFactory.getInstance("X.509");

        for (CACertificate cert : CACertificate.getAll()) {
            if ("root".equals(cert.getKeyname())) {
                root = cert;
                rootId = cert.getId();
            } else if ("orga".equals(cert.getKeyname())) {
                orga = cert;
                orgaId = cert.getId();
            }
        }
        FileInputStream fis = new FileInputStream(new File("config/ca/root.crt"));
        configRoot = (X509Certificate) fact.generateCertificate(fis);
    }

    @Test
    public void testGetParent() {
        assertEquals(root, orga.getParent());
    }

    @Test
    public void testGetCertificate() {
        assertEquals(configRoot, root.getCertificate());
    }

    @Test
    public void testToString() {
        assertEquals("CACertificate: root", root.toString());
        assertEquals("CACertificate: orga", orga.toString());
    }

    @Test
    public void testGetId() {
        assertEquals(rootId, root.getId());
        assertEquals(orgaId, orga.getId());
    }

    @Test
    public void testGetKeyname() {
        assertEquals("root", root.getKeyname());
        assertEquals("orga", orga.getKeyname());
    }

    // TODO: test getLink

    @Test
    public void testGetById() {
        assertEquals(root, CACertificate.getById(rootId));
        assertEquals(orga, CACertificate.getById(orgaId));
    }

    @Test
    public void testIsSelfsigned() {
        assertTrue(root.isSelfsigned());
        assertFalse(orga.isSelfsigned());
    }

    @Test
    public void testGetFingerprint() throws CertificateEncodingException, NoSuchAlgorithmException {
        assertEquals(Certificate.getFingerprint(configRoot, "sha-1"), root.getFingerprint("sha-1"));
        assertEquals(Certificate.getFingerprint(configRoot, "sha-256"), root.getFingerprint("sha-256"));
    }

    @Test
    public void testGetAll() throws FileNotFoundException, CertificateException {
        for (CACertificate cert : CACertificate.getAll()) {
            FileInputStream fis = new FileInputStream(new File(String.format("config/ca/%s.crt", cert.getKeyname())));
            assertEquals(cert.getCertificate(), (X509Certificate) fact.generateCertificate(fis));
        }
    }
}
