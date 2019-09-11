package club.wpia.gigi.pages;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;

public class TestMain extends ClientTest {

    @Test
    public void testPasswordLogin() throws MalformedURLException, IOException {
        URLConnection uc = new URL("https://" + getServerName()).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String content = IOUtils.readURL(uc);

        assertThat(content, not(containsString("via certificate")));

        makeAgent(u.getId());
        uc = new URL("https://" + getServerName()).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        content = IOUtils.readURL(uc);
        assertThat(content, containsString("For some actions, e.g. add verification, support, you need to be authenticated via certificate."));

    }

    @Test
    public void testCertLogin() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=" + u.getPreferredName().toString());
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", u.getPreferredName().toString()), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
        final PrivateKey pk = kp.getPrivate();
        await(c.issue(null, "2y", u));
        final X509Certificate ce = c.cert();
        c.setLoginEnabled(true);
        cookie = login(pk, ce);
        loginCertificate = c;
        loginPrivateKey = pk;

        URLConnection uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        String content = IOUtils.readURL(uc);
        assertThat(content, not(containsString("via certificate")));

        makeAgent(u.getId());
        uc = new URL("https://" + getSecureServerName()).openConnection();
        authenticate((HttpURLConnection) uc);
        content = IOUtils.readURL(uc);
        assertThat(content, containsString("You are authenticated via certificate, so you will be able to perform all actions."));

    }
}
