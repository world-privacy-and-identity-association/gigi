package club.wpia.gigi.email;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;

import club.wpia.gigi.testUtils.ConfiguredTest;

public class TestEmailProviderClass extends ConfiguredTest {

    @Test
    public void testNonmail() throws IOException {
        String result = EmailProvider.getInstance().checkEmailServer(0, "nomail");
        assertNotEquals(EmailProvider.OK, result);
    }

    @Test
    public void testFastcheckSucceed() throws IOException {
        String succmail = getTestProps().getProperty("email.address");
        assumeNotNull(succmail);

        String result = EmailProvider.getInstance().checkEmailServer(0, succmail);
        assertEquals(EmailProvider.OK, result);
    }

    @Test
    public void testFastcheckFail() throws IOException {
        String failmail = getTestProps().getProperty("email.non-address");
        assumeNotNull(failmail);

        String result = EmailProvider.getInstance().checkEmailServer(0, failmail);
        assertNotEquals(EmailProvider.OK, result);
    }

    @BeforeClass
    public static void initMailsystem() throws NoSuchAlgorithmException, KeyManagementException {
        Properties prop = new Properties();
        prop.setProperty("emailProvider", "club.wpia.gigi.email.SendMail");
        EmailProvider.initSystem(prop, null, null);
        SSLContext c = SSLContext.getInstance("TLS");
        c.init(null, new TrustManager[] {
                new X509TrustManager() {

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

                }

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

                }
                }
        }, null);
        SSLContext.setDefault(c);
    }
}
