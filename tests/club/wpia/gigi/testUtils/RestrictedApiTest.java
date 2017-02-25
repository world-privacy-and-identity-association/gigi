package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.junit.BeforeClass;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.dbObjects.Country.CountryCodeType;
import club.wpia.gigi.util.ServerConstants;

public class RestrictedApiTest extends ClientTest {

    protected static PrivateKey pk;

    protected static X509Certificate ce;

    protected static Organisation selfOrg;

    public RestrictedApiTest() {
        makeAgent(id);
    }

    @BeforeClass
    public static void initCert() {
        initEnvironment();
        try {
            User u = User.getById(createVerificationUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
            grant(u, Group.ORG_AGENT);
            clearCaches();
            u = User.getById(u.getId());
            selfOrg = new Organisation(Organisation.SELF_ORG_NAME, Country.getCountryByCode("DE", CountryCodeType.CODE_2_CHARS), "NA", "NA", "contact@example.org", "", "", u);
            assertTrue(selfOrg.isSelfOrganisation());
            KeyPair kp = generateKeypair();
            String key1 = generatePEMCSR(kp, "EMAIL=" + ServerConstants.getQuizMailAddress());
            Certificate apiCert = new Certificate(selfOrg, u, Certificate.buildDN("EMAIL", ServerConstants.getQuizMailAddress()), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getByName("client-orga"), new Certificate.SubjectAlternateName(SANType.EMAIL, ServerConstants.getQuizMailAddress()));
            pk = kp.getPrivate();
            await(apiCert.issue(null, "2y", u));
            ce = apiCert.cert();
            apiCert.setLoginEnabled(true);
        } catch (IOException e) {
            throw new Error(e);
        } catch (GigiApiException e) {
            throw new Error(e);
        } catch (GeneralSecurityException e) {
            throw new Error(e);
        } catch (InterruptedException e) {
            throw new Error(e);
        }

    }

    public HttpURLConnection doApi(String path, String content) throws IOException, GeneralSecurityException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getServerName().replaceFirst("^www.", "api.") + path).openConnection();
        authenticateClientCert(pk, ce, connection);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(content.getBytes());
        return connection;
    }
}
