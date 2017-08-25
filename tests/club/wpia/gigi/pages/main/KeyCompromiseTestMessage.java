package club.wpia.gigi.pages.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.HTMLEncoder;
import club.wpia.gigi.util.PEM;

public class KeyCompromiseTestMessage extends ClientTest {

    private Certificate cert;

    private PrivateKey priv;

    public KeyCompromiseTestMessage() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        KeyPair kp = generateKeypair();
        priv = kp.getPrivate();
        String csr = generatePEMCSR(kp, "CN=test");
        CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), csr);
        cr.update(CertificateRequest.DEFAULT_CN, Digest.SHA512.toString(), "client", null, null, "email:" + email + "\n");
        cert = cr.draft();
        Job j = cert.issue(null, "2y", u);
        await(j);
    }

    @Test
    public void testExecution() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        reportCompromiseAndCheck("");
    }

    @Test
    public void testNoConfidential() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=test+message");
        assertThat(rc.getMessage(), CoreMatchers.containsString("test message"));
    }

    @Test
    public void testNoConfidentialButMarker() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=" + URLEncoder.encode(KeyCompromiseForm.CONFIDENTIAL_MARKER + "\ntest message", "UTF-8"));
        assertThat(rc.getMessage(), CoreMatchers.containsString("test message"));
        assertThat(rc.getMessage(), CoreMatchers.containsString(" " + KeyCompromiseForm.CONFIDENTIAL_MARKER));
    }

    @Test
    public void testConfidential() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=test+message&confidential=on");
        assertThat(rc.getMessage(), CoreMatchers.not(CoreMatchers.containsString("test message")));
    }

    @Test
    public void testCR() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=test%0Dmessage&confidential=on");
        assertThat(rc.getMessage(), CoreMatchers.not(CoreMatchers.containsString("test\r\nmessage")));
    }

    @Test
    public void testLF() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=test%0Amessage&confidential=on");
        assertThat(rc.getMessage(), CoreMatchers.not(CoreMatchers.containsString("test\r\nmessage")));
    }

    @Test
    public void testCRLF() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        TestMail rc = reportCompromiseAndCheck("message=test%0D%0Amessage&confidential=on");
        assertThat(rc.getMessage(), CoreMatchers.not(CoreMatchers.containsString("test\r\nmessage")));
    }

    @Test
    public void testIllegalContent() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        HttpURLConnection rc = reportCompromise("message=test+message+---&confidential=on");
        String data = IOUtils.readURL(rc);
        assertThat(data, hasError());
        assertThat(data, CoreMatchers.containsString(HTMLEncoder.encodeHTML("message may not contain '---'")));
        assertNull(getMailReceiver().poll());
        assertEquals(CertificateStatus.ISSUED, cert.getStatus());
    }

    @Test
    public void testIllegalChars() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        HttpURLConnection rc = reportCompromise("message=" + URLEncoder.encode("§", "UTF-8"));
        String data = IOUtils.readURL(rc);
        assertThat(data, hasError());
        assertThat(data, CoreMatchers.containsString("may only contain printable ASCII characters"));
        assertEquals(CertificateStatus.ISSUED, cert.getStatus());
    }

    private TestMail reportCompromiseAndCheck(String params) throws IOException, UnsupportedEncodingException, CertificateEncodingException, GeneralSecurityException {
        HttpURLConnection huc = reportCompromise(params);
        assertThat(IOUtils.readURL(huc), hasNoError());
        TestMail rc = getMailReceiver().receive();
        assertEquals(u.getEmail(), rc.getTo());
        assertThat(rc.getMessage(), CoreMatchers.containsString(cert.getSerial()));
        assertEquals(CertificateStatus.REVOKED, cert.getStatus());
        return rc;
    }

    private HttpURLConnection reportCompromise(String params) throws IOException, UnsupportedEncodingException, CertificateEncodingException, GeneralSecurityException {
        if ( !params.isEmpty() && !params.startsWith("&")) {
            params = "&" + params;
        }
        HttpURLConnection huc = post(KeyCompromisePage.PATH, "cert=" + URLEncoder.encode(PEM.encode("CERTIFICATE", cert.cert().getEncoded()), "UTF-8")//
                + "&priv=" + URLEncoder.encode(PEM.encode("PRIVATE KEY", priv.getEncoded()), "UTF-8") + params);
        return huc;
    }
}
