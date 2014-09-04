package org.cacert.gigi.ping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.pages.account.DomainOverview;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.PingTest;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.junit.Test;

public class TestSSL extends PingTest {

    private KeyPair kp;

    private Certificate c;

    @Test
    public void sslAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(0, 0, true);
    }

    @Test
    public void sslWongTypeAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(1, 0, true);
    }

    @Test
    public void sslOneMissingAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(2, 0, true);
    }

    @Test
    public void sslBothMissingAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(3, 0, true);
    }

    @Test
    public void sslWrongTypeAndMailFail() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(1, 1, false);
    }

    /**
     * @param sslVariant
     *            <ul>
     *            <li>0= all valid</li>
     *            <li>1= wrong type</li>
     *            <li>2= one server missing</li>
     *            <li>3= both servers missing</li>
     *            </ul>
     * @param emailVariant
     * @param successSSL
     * @param successMail
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     * @throws GeneralSecurityException
     * @throws GigiApiException
     */

    private void testEmailAndSSL(int sslVariant, int emailVariant, boolean successMail) throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        String test = getTestProps().getProperty("domain.local");

        URL u = new URL("https://" + getServerName() + DomainOverview.PATH);

        initailizeDomainForm(u);

        createCertificate(test, CertificateProfile.getByName(sslVariant == 1 ? "client" : "server"));
        SSLServerSocket sss = createSSLServer(kp.getPrivate(), c.cert());
        int port = sss.getLocalPort();
        SSLServerSocket sss2 = createSSLServer(kp.getPrivate(), c.cert());
        int port2 = sss2.getLocalPort();
        if (sslVariant == 3 || sslVariant == 2) {
            sss2.close();
            if (sslVariant == 3) {
                sss.close();
            }
        }
        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&SSLType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + port + //
                "&ssl-type-1=direct&ssl-port-1=" + port2 + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        URL u2 = sendDomainForm(u, content);
        boolean firstSucceeds = sslVariant != 0 && sslVariant != 2;
        assertTrue(firstSucceeds ^ acceptSSLServer(sss));
        boolean secondsSucceeds = sslVariant != 0;
        assertTrue(secondsSucceeds ^ acceptSSLServer(sss2));

        TestMail mail = getMailReciever().recieve();
        if (emailVariant == 0) {
            String link = mail.extractLink();
            new URL(link).openConnection().getHeaderField("");
        }
        waitForPings(3);

        String newcontent = IOUtils.readURL(cookie(u2.openConnection(), cookie));
        Pattern pat = Pattern.compile("<td>ssl</td>\\s*<td>success</td>");
        Matcher matcher = pat.matcher(newcontent);
        assertTrue(newcontent, firstSucceeds ^ matcher.find());
        assertTrue(newcontent, secondsSucceeds ^ matcher.find());
        assertFalse(newcontent, matcher.find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successMail ^ pat.matcher(newcontent).find());
    }

    private void createCertificate(String test, CertificateProfile profile) throws GeneralSecurityException, IOException, SQLException, InterruptedException, GigiApiException {
        kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=" + test);
        c = new Certificate(userid, "/CN=" + test, "sha256", csr, CSRType.CSR, profile);
        c.issue(null, "2y").waitFor(60000);
    }

    private boolean acceptSSLServer(SSLServerSocket sss) throws IOException {
        try {
            Socket s = sss.accept();
            s.getOutputStream().write('b');
            s.getOutputStream().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private SSLServerSocket createSSLServer(final PrivateKey priv, final X509Certificate cert) throws Error, IOException {
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(new KeyManager[] {
                new X509KeyManager() {

                    @Override
                    public String[] getServerAliases(String keyType, Principal[] issuers) {
                        return new String[] {
                            "server"
                        };
                    }

                    @Override
                    public PrivateKey getPrivateKey(String alias) {
                        return priv;
                    }

                    @Override
                    public String[] getClientAliases(String keyType, Principal[] issuers) {
                        throw new Error();
                    }

                    @Override
                    public X509Certificate[] getCertificateChain(String alias) {
                        return new X509Certificate[] {
                            cert
                        };
                    }

                    @Override
                    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                        throw new Error();
                    }

                    @Override
                    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                        return "server";
                    }

                }
            }, new TrustManager[] {
                new X509TrustManager() {

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                }
            }, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new Error(e);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            throw new Error(e);
        }

        SSLServerSocketFactory sssf = sc.getServerSocketFactory();
        return (SSLServerSocket) sssf.createServerSocket(0);
    }

}
