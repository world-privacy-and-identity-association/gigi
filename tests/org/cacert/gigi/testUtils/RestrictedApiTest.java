package org.cacert.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.CountryCode;
import org.cacert.gigi.dbObjects.CountryCode.CountryCodeType;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.junit.BeforeClass;

public class RestrictedApiTest extends ClientTest {

    protected static PrivateKey pk;

    protected static X509Certificate ce;

    public RestrictedApiTest() {
        makeAssurer(id);
    }

    @BeforeClass
    public static void initCert() {
        initEnvironment();
        try {
            User u = User.getById(createAssuranceUser("f", "l", createUniqueName() + "@email.com", TEST_PASSWORD));
            grant(u.getEmail(), Group.ORGASSURER);
            clearCaches();
            u = User.getById(u.getId());
            Organisation o = new Organisation(Organisation.SELF_ORG_NAME, CountryCode.getCountryCode("DE", CountryCodeType.CODE_2_CHARS), "NA", "NA", "contact@cacert.org", "", "", u);
            assertTrue(o.isSelfOrganisation());
            KeyPair kp = generateKeypair();
            String key1 = generatePEMCSR(kp, "EMAIL=cats@cacert.org");
            Certificate c = new Certificate(o, u, Certificate.buildDN("EMAIL", "cats@cacert.org"), Digest.SHA256, key1, CSRType.CSR, CertificateProfile.getByName("client-orga"), new Certificate.SubjectAlternateName(SANType.EMAIL, "cats@cacert.org"));
            pk = kp.getPrivate();
            await(c.issue(null, "2y", u));
            ce = c.cert();
            c.setLoginEnabled(true);
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
