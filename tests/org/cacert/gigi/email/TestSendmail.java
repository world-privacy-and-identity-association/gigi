package org.cacert.gigi.email;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import javax.net.ssl.SSLSocketFactory;

import org.cacert.gigi.testUtils.ConfiguredTest;
import org.junit.Test;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class TestSendmail extends ConfiguredTest {

    private static final Random rng = new Random();

    @Test
    public void testSendmail() throws IOException, GeneralSecurityException {
        initSelfsign();

        String succmail = getTestProps().getProperty("email.address");
        String pass = getTestProps().getProperty("email.password");
        String imap = getTestProps().getProperty("email.imap");
        String imapuser = getTestProps().getProperty("email.imap.user");
        assumeNotNull(succmail, pass, imap, imapuser);

        String subj = "subj-" + createUniqueName();
        String msg = "msg-" + createUniqueName();
        EmailProvider.getInstance().sendmail(succmail, subj, msg, "system@cacert.org", "system@cacert.org", "Testtarget", "Testsender", null, false);

        try (Socket s = SSLSocketFactory.getDefault().createSocket(imap, 993);//
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);//
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"))) {
            pw.println("a001 login " + imapuser + " " + pass);
            imapUntil(br, "a001");
            pw.println("a002 select inbox");
            String overview = imapUntil(br, "a002");
            overview = overview.replaceFirst(".*\\* ([0-9]+) EXISTS.*", "$1");
            int cont = Integer.parseInt(overview);

            int msgid = -1;
            for (int i = 1; i <= cont; i++) {
                pw.println("m003" + i + " fetch " + i + " body[header]");
                String body = imapUntil(br, "m003" + i);
                if (body.contains(subj)) {
                    msgid = i;
                    break;
                }
            }
            assertNotEquals( -1, msgid);
            pw.println("a003 fetch " + msgid + " body[]");
            String body = imapUntil(br, "a003");
            pw.println("delete store " + msgid + " +flags \\deleted");
            imapUntil(br, "delete");
            pw.println("exp expunge");
            imapUntil(br, "exp");
            pw.println("log logout");
            imapUntil(br, "log");
            assertThat(body, containsString("From: support@cacert.local"));
            assertThat(body, containsString("To: gigi-testuser@dogcraft.de"));
            assertThat(body, containsString("Subject: " + subj));
            assertThat(body, containsString(Base64.getEncoder().encodeToString(msg.getBytes("UTF-8"))));

            // TODO maybe verify signature
        }
    }

    private String imapUntil(BufferedReader br, String target) throws IOException {
        StringBuffer response = new StringBuffer();
        String line = "";
        while ( !line.startsWith(target)) {
            line = br.readLine();
            if (line == null) {
                throw new EOFException();
            }
            response.append(line);
        }
        return response.toString();
    }

    private void initSelfsign() throws GeneralSecurityException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        Properties prop = new Properties();
        prop.setProperty("emailProvider", "org.cacert.gigi.email.Sendmail");
        KeyPair kp = generateKeypair();
        X509CertInfo info = new X509CertInfo();
        // Add all mandatory attributes
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(rng.nextInt() & 0x7fffffff));
        AlgorithmId algID = AlgorithmId.get("SHA256WithRSA");
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algID));
        info.set(X509CertInfo.SUBJECT, new X500Name("EMAIL=system@cacert.org"));
        info.set(X509CertInfo.KEY, new CertificateX509Key(kp.getPublic()));
        info.set(X509CertInfo.VALIDITY, new CertificateValidity(new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 60 * 60 * 1000)));
        info.set(X509CertInfo.ISSUER, new X500Name("CN=test-issue"));
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(kp.getPrivate(), "SHA256WithRSA");
        EmailProvider.initSystem(prop, cert, kp.getPrivate());
    }
}
