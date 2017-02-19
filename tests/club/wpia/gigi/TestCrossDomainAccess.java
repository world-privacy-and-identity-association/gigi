package club.wpia.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.sql.SQLException;

import org.junit.Test;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.ManagedTest;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestCrossDomainAccess extends ManagedTest {

    @Test
    public void testNoOriginHeader() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/login").openConnection();
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToHttps() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/login").openConnection();
        con.setRequestProperty("Origin", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW));
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpToHttps() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/login").openConnection();
        con.setRequestProperty("Origin", "http://" + ServerConstants.getHostNamePort(Host.WWW));
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToSecure() throws MalformedURLException, IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        User u = User.getById(createVerifiedUser("fn", "ln", "testmail@example.com", TEST_PASSWORD));
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=testmail@example.com");
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        c.setLoginEnabled(true);
        await(c.issue(null, "2y", u));

        URLConnection con = new URL("https://" + ServerConstants.getHostNamePortSecure(Host.SECURE)).openConnection();
        authenticateClientCert(pk, c.cert(), (HttpURLConnection) con);
        con.setRequestProperty("Origin", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW));
        String contains = IOUtils.readURL(con);
        assertTrue( !contains.contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToHttp() throws MalformedURLException, IOException {
        URLConnection con = new URL("http://" + ServerConstants.getHostNamePort(Host.WWW)).openConnection();
        con.setRequestProperty("Origin", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW));
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testIncorrectOriginHeader() throws MalformedURLException, IOException {
        HttpURLConnection con = (HttpURLConnection) new URL("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/login").openConnection();
        con.setRequestProperty("Origin", "https://evilpageandatleastnotcacert.com");
        assertTrue(IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

}
