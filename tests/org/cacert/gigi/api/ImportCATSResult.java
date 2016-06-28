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
import org.cacert.gigi.testUtils.IOUtils;
import org.junit.Test;

public class ImportCATSResult extends ClientTest {

    private PrivateKey pk;

    private X509Certificate ce;

    public ImportCATSResult() throws IOException, GeneralSecurityException, InterruptedException, GigiApiException {
        makeAssurer(id);

        grant(u.getEmail(), Group.ORGASSURER);
        clearCaches();
        u = User.getById(u.getId());
        Organisation o = new Organisation(Organisation.SELF_ORG_NAME, "NA", "NA", "NA", "contact@cacert.org", u);
        assertTrue(o.isSelfOrganisation());
        KeyPair kp = generateKeypair();
        String key1 = generatePEMCSR(kp, "EMAIL=cats@cacert.org");
        Certificate c = new Certificate(o, u, Certificate.buildDN("EMAIL", "cats@cacert.org"), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getByName("client-orga"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        pk = kp.getPrivate();
        c.issue(null, "2y", u).waitFor(60000);
        ce = c.cert();
    }

    @Test
    public void testLookupSerial() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        Certificate target2 = new Certificate(u, u, Certificate.buildDN("EMAIL", u.getEmail()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + u.getEmail()), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
        target2.issue(null, "2y", u).waitFor(60000);

        assertEquals(u.getId(), Integer.parseInt(apiLookup(target2)));
    }

    @Test
    public void testImportCATS() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {

        assertEquals(1, u.getTrainings().length);
        apiImport(u, "Test Training");
        assertEquals(2, u.getTrainings().length);

        User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "@example.com", TEST_PASSWORD));
        assertEquals(0, u2.getTrainings().length);
        assertFalse(u2.hasPassedCATS());
        apiImport(u2, "Test Training");
        assertEquals(1, u2.getTrainings().length);
        assertFalse(u2.hasPassedCATS());
        apiImport(u2, CATS.ASSURER_CHALLENGE_NAME);
        assertEquals(2, u2.getTrainings().length);
        assertTrue(u2.hasPassedCATS());

    }

    @Test
    public void testImportCATSFailures() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        assertEquals(1, u.getTrainings().length);
        assertNotEquals(200, executeImportQuery("").getResponseCode());
        assertNotEquals(200, executeImportQuery("mid=" + u.getId()).getResponseCode());
        assertNotEquals(200, executeImportQuery("mid=" + u.getId() + "&variant=Test+Training").getResponseCode());
        assertNotEquals(200, executeImportQuery("mid=" + u.getId() + "&variant=Test+Training&date=" + System.currentTimeMillis()).getResponseCode());
        assertNotEquals(200, executeImportQuery("mid=" + u.getId() + "&variant=Test+Training&date=" + System.currentTimeMillis() + "&language=en").getResponseCode());
        assertNotEquals(200, executeImportQuery("mid=" + u.getId() + "&variant=Test+Training&date=" + System.currentTimeMillis() + "&version=1.0").getResponseCode());
        assertEquals(1, u.getTrainings().length);
        apiImport(u, "Test Training");
        assertEquals(2, u.getTrainings().length);

    }

    private void apiImport(User target, String test) throws IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException, UnsupportedEncodingException, GeneralSecurityException {
        HttpURLConnection connection = executeImportQuery("mid=" + target.getId() + "&variant=" + URLEncoder.encode(test, "UTF-8") + "&date=" + System.currentTimeMillis() + "&language=en&version=1.0");
        if (connection.getResponseCode() != 200) {
            throw new Error(connection.getResponseMessage());
        }
    }

    private HttpURLConnection executeImportQuery(String query) throws IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException, UnsupportedEncodingException, Error {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + CATSImport.PATH).openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(query.getBytes("UTF-8"));
        return connection;
    }

    private String apiLookup(Certificate target) throws IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException, UnsupportedEncodingException, GeneralSecurityException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + CATSResolve.PATH).openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(("serial=" + target.cert().getSerialNumber().toString(16).toLowerCase()).getBytes());
        if (connection.getResponseCode() != 200) {
            throw new Error(connection.getResponseMessage());
        }
        return IOUtils.readURL(connection);
    }
}
