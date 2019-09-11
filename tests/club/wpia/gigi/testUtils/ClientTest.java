package club.wpia.gigi.testUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.User;

/**
 * Superclass for testsuites in a scenario where there is an registered member,
 * who is already logged on.
 */
public abstract class ClientTest extends ManagedTest {

    /**
     * Email of the member.
     */
    protected String email = createUniqueName() + "@example.org";

    /**
     * Id of the member
     */
    protected int id = createVerifiedUser("a", "b", email, TEST_PASSWORD);

    /**
     * {@link User} object of the member
     */
    protected User u = User.getById(id);

    /**
     * Session cookie of the member.
     */
    protected String cookie;

    public ClientTest() {
        try {
            cookie = login(email, TEST_PASSWORD);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public HttpURLConnection post(String path, String query) throws IOException {
        return post(path, query, 0);
    }

    public HttpURLConnection post(String path, String query, int formIndex) throws IOException {
        String server = getServerName();
        if (loginCertificate != null) {
            server = getSecureServerName();
        }
        URLConnection uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc);
        String csrf = getCSRF(uc, formIndex);

        uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + query//
        ).getBytes("UTF-8"));
        os.flush();
        return (HttpURLConnection) uc;
    }

    public HttpURLConnection get(String path) throws IOException {
        String server = getServerName();
        if (loginCertificate != null) {
            server = getSecureServerName();
        }
        URLConnection uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc);
        return (HttpURLConnection) uc;
    }

    protected void authenticate(HttpURLConnection uc) throws IOException {
        uc.addRequestProperty("Cookie", cookie);
        if (loginCertificate != null) {
            try {
                authenticateClientCert(loginPrivateKey, loginCertificate.cert(), uc);
            } catch (GeneralSecurityException | GigiApiException e) {
                throw new IOException(e);
            }
        }
    }

}
