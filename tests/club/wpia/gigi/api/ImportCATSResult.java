package club.wpia.gigi.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.api.CATSImport;
import club.wpia.gigi.api.CATSResolve;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.RestrictedApiTest;
import club.wpia.gigi.util.ServerConstants;

public class ImportCATSResult extends RestrictedApiTest {

    @Test
    public void testLookupSerial() throws GigiApiException, IOException, GeneralSecurityException, InterruptedException {
        Certificate target2 = new Certificate(u, u, Certificate.buildDN("EMAIL", u.getEmail()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + u.getEmail()), CSRType.CSR, CertificateProfile.getByName("client"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@example.com"));
        await(target2.issue(null, "2y", u));
        target2.setLoginEnabled(true);

        assertEquals(u.getId(), Integer.parseInt(apiLookup(target2)));

        Certificate target3 = new Certificate(selfOrg, u, Certificate.buildDN("EMAIL", ServerConstants.getQuizAdminMailAddress()), Digest.SHA256, generatePEMCSR(generateKeypair(), "EMAIL=" + ServerConstants.getQuizAdminMailAddress()), CSRType.CSR, CertificateProfile.getByName("client-orga"), new Certificate.SubjectAlternateName(SANType.EMAIL, ServerConstants.getQuizAdminMailAddress()));
        await(target3.issue(null, "2y", u));
        target3.setLoginEnabled(true);

        assertEquals("admin", apiLookup(target3));
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
        apiImport(u2, CATSType.ASSURER_CHALLENGE.getDisplayName());
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

    private HttpURLConnection executeImportQuery(String query) throws IOException, GeneralSecurityException {
        return doApi(CATSImport.PATH, query);
    }

    private String apiLookup(Certificate target) throws IOException, GeneralSecurityException {
        HttpURLConnection connection = doApi(CATSResolve.PATH, "serial=" + target.cert().getSerialNumber().toString(16).toLowerCase());
        if (connection.getResponseCode() != 200) {
            throw new Error(connection.getResponseMessage());
        }
        return IOUtils.readURL(connection);
    }

}
