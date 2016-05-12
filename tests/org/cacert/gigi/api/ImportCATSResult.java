package org.cacert.gigi.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CATS;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class ImportCATSResult extends ClientTest {

    @Test
    public void testImportCATS() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        makeAssurer(id);
        Certificate target = new Certificate(u, u, Certificate.buildDN("EMAIL", email), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + email), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        target.issue(null, "2y", u).waitFor(60000);

        grant(u.getEmail(), Group.ORGASSURER);
        clearCaches();
        u = User.getById(u.getId());
        Organisation o = new Organisation(Organisation.SELF_ORG_NAME, "NA", "NA", "NA", "contact@cacert.org", u);
        assertTrue(o.isSelfOrganisation());
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "EMAIL=cats@cacert.org");
        Certificate c = new Certificate(o, u, Certificate.buildDN("EMAIL", "cats@cacert.org"), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getByName("client-orga"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        final PrivateKey pk = kp.getPrivate();
        c.issue(null, "2y", u).waitFor(60000);
        final X509Certificate ce = c.cert();

        assertEquals(1, u.getTrainings().length);
        apiRequest(target.cert().getSerialNumber().toString(16), "Test Training", pk, ce);
        assertEquals(2, u.getTrainings().length);

        User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));
        Certificate target2 = new Certificate(u2, u2, Certificate.buildDN("EMAIL", u2.getEmail()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + u2.getEmail()), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        target2.issue(null, "2y", u).waitFor(60000);
        assertEquals(0, u2.getTrainings().length);
        assertFalse(u2.hasPassedCATS());
        apiRequest(target2.cert().getSerialNumber().toString(16), "Test Training", pk, ce);
        assertEquals(1, u2.getTrainings().length);
        assertFalse(u2.hasPassedCATS());
        apiRequest(target2.cert().getSerialNumber().toString(16), CATS.ASSURER_CHALLANGE_NAME, pk, ce);
        assertEquals(2, u2.getTrainings().length);
        assertTrue(u2.hasPassedCATS());

    }

    private void apiRequest(String target, String test, final PrivateKey pk, final X509Certificate ce) throws IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException, UnsupportedEncodingException, GeneralSecurityException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + CATSImport.PATH).openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(("serial=" + target + "&variant=" + URLEncoder.encode(test, "UTF-8") + "&date=" + System.currentTimeMillis()).getBytes("UTF-8"));
        System.out.println(connection.getResponseCode());
        System.out.println(connection.getResponseMessage());
    }
}
