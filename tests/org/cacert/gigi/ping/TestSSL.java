package org.cacert.gigi.ping;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.DatabaseConnection.Link;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Job;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.PingTest;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.cacert.gigi.util.SimpleSigner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestSSL extends PingTest {

    @Parameters(name = "self-signed = {0}")
    public static Iterable<Object[]> genParams() throws IOException {
        return Arrays.asList(new Object[] {
                true
        }, new Object[] {
                false
        });

    }

    @Parameter
    public Boolean self = false;

    public abstract static class AsyncTask<T> {

        T res;

        Thread runner;

        Exception ex;

        public T join() throws InterruptedException {
            runner.join();
            if (ex != null) {
                throw new Error(ex);
            }
            return res;
        }

        public void start() {
            runner = new Thread() {

                @Override
                public void run() {
                    try {
                        res = AsyncTask.this.run();
                    } catch (Exception e) {
                        ex = e;
                    }
                }
            };
            runner.start();
        }

        public abstract T run() throws Exception;

    }

    private KeyPair kp;

    private X509Certificate c;

    @Test(timeout = 70000)
    public void sslAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(0, 0, true);
    }

    @Test(timeout = 70000)
    public void sslWongTypeAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(1, 0, true);
    }

    @Test(timeout = 70000)
    public void sslOneMissingAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(2, 0, true);
    }

    @Test(timeout = 70000)
    public void sslBothMissingAndMailSuccess() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(3, 0, true);
    }

    @Test(timeout = 70000)
    public void sslWrongTypeAndMailFail() throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        testEmailAndSSL(1, 1, false);
    }

    private void testEmailAndSSL(int sslVariant, int emailVariant, boolean successMail) throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        try (Link link = DatabaseConnection.newLink(false)) {
            testEmailAndSSLWithLink(sslVariant, emailVariant, successMail);
        }
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

    private void testEmailAndSSLWithLink(int sslVariant, int emailVariant, boolean successMail) throws IOException, InterruptedException, SQLException, GeneralSecurityException, GigiApiException {
        String test = getTestProps().getProperty("domain.local");
        assumeNotNull(test);
        Matcher m = initailizeDomainForm();
        String value = m.group(2);

        if (self) {
            createCertificateSelf(test, sslVariant == 1 ? "clientAuth" : "serverAuth", value);
        } else {
            createCertificate(test, CertificateProfile.getByName(sslVariant == 1 ? "client" : "server"));
        }

        final SSLServerSocket sss = createSSLServer(kp.getPrivate(), c);
        int port = sss.getLocalPort();
        final SSLServerSocket sss2 = createSSLServer(kp.getPrivate(), c);
        int port2 = sss2.getLocalPort();
        if (sslVariant == 3 || sslVariant == 2) {
            sss2.close();
            if (sslVariant == 3) {
                sss.close();
            }
        }
        String content = "adddomain&newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&SSLType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + port + //
                "&ssl-type-1=direct&ssl-port-1=" + port2 + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        String p2 = sendDomainForm(content);
        boolean firstSucceeds = sslVariant != 0 && sslVariant != 2;
        AsyncTask<Boolean> ass = new AsyncTask<Boolean>() {

            @Override
            public Boolean run() throws Exception {
                return acceptSSLServer(sss);
            }
        };
        ass.start();
        System.out.println(port + " and " + port2 + " ready");
        System.err.println(port + " and " + port2 + " ready");
        boolean accept2 = acceptSSLServer(sss2);
        boolean accept1 = ass.join();
        // assertTrue(firstSucceeds ^ accept1);
        boolean secondsSucceeds = sslVariant != 0;
        // assertTrue(secondsSucceeds ^ accept2);

        TestMail mail = getMailReciever().receive();
        if (emailVariant == 0) {
            mail.verify();
        }
        waitForPings(3);

        String newcontent = IOUtils.readURL(get(p2));
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
        User u = User.getById(id);
        Certificate c = new Certificate(u, u, Certificate.buildDN("CN", test), Digest.SHA256, csr, CSRType.CSR, profile);
        Job j = c.issue(null, "2y", u);
        await(j);
        this.c = c.cert();
    }

    private void createCertificateSelf(String test, String eku, String tok) throws GeneralSecurityException, IOException, SQLException, InterruptedException, GigiApiException {
        kp = generateKeypair();
        HashMap<String, String> name = new HashMap<>();
        name.put("CN", "");
        name.put("OU", tok);

        Date from = new Date();
        Date to = new Date(from.getTime() + 1000 * 60 * 60 * 2);
        List<Certificate.SubjectAlternateName> l = new LinkedList<>();

        byte[] cert = SimpleSigner.generateCert(kp.getPublic(), kp.getPrivate(), name, new X500Principal(SimpleSigner.genX500Name(name).getEncoded()), l, from, to, Digest.SHA256, eku);
        c = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(cert));
    }

    private boolean acceptSSLServer(SSLServerSocket sss) throws IOException {
        try (Socket s = sss.accept()) {
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

    public static void main(String[] args) throws Exception {
        initEnvironment();
        TestSSL t1 = new TestSSL();
        t1.sslAndMailSuccess();
        tearDownServer();
    }

}
