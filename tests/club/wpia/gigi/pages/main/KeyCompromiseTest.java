package club.wpia.gigi.pages.main;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.PEM;

@RunWith(Parameterized.class)
public class KeyCompromiseTest extends ClientTest {

    private static class TestParameters {

        private final String query;

        private final String error;

        public TestParameters(String query, String error) {
            this.query = query;
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public String getQuery() {
            return query;
        }

        @Override
        public String toString() {
            return query + ": " + error;
        }
    }

    private Certificate cert;

    private String serial;

    private PrivateKey priv;

    private TestParameters pm;

    public KeyCompromiseTest(TestParameters pm) throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        this.pm = pm;
        KeyPair kp = generateKeypair();
        priv = kp.getPrivate();
        String csr = generatePEMCSR(kp, "CN=test");
        CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), csr);
        cr.update(CertificateRequest.DEFAULT_CN, Digest.SHA512.toString(), "client", null, null, "email:" + email + "\n");
        cert = cr.draft();
        Job j = cert.issue(null, "2y", u);
        await(j);
        serial = cert.getSerial();
    }

    @Parameters(name = "{0}")
    public static Object[][] getParams() {
        return new Object[][] {
                params("serial=%serial&priv=%priv", null),// serial+key
                params("serial=0000%serial&priv=%priv", null),// leading Zeros
                params("serial=0000%Serial&priv=%priv", null),// upper case
                params("cert=%cert&priv=%priv", null),// cert+key
                params("serial=%serial&signature=%signature", null),
                // Zero serial
                params("serial=0000&priv=%priv", KeyCompromiseForm.NOT_FOUND.getRaw()),
                params("serial=0lkd&priv=%priv", KeyCompromiseForm.NOT_FOUND.getRaw()),
                // tampered cert
                params("cert=%tamperedCert&priv=%priv", "not be parsed"),
                params("cert=%cert&priv=%tamperedPriv", "Private Key is malformed"),
                params("serial=1&priv=%priv", KeyCompromiseForm.NOT_FOUND.getRaw()),
                params("serial=1%serial&priv=%priv", KeyCompromiseForm.NOT_FOUND.getRaw()),
                // missing certificate identification
                params("serial=&cert=&priv=%priv", "identification"),
                params("cert=&priv=%priv", "identification"),
                params("serial=&priv=%priv", "identification"),
                params("priv=%priv", "identification"),
                // sign missing
                params("serial=%serial&priv=&signature=", "No verification"),
                params("serial=%serial&signature=", "No verification"),
                params("serial=%serial&priv=", "No verification"),
                params("serial=%serial", "No verification"),
                params("cert=%cert&signature=%tamperedSignature", "Verification does not match"),

                params("cert=-_&signature=%signature", "certificate could not be parsed"),
                params("cert=%cert&signature=-_", "Signature is malformed"),
                params("cert=%cert&priv=-_", "Private Key is malformed"),
        };
    }

    private static Object[] params(String query, String error) {
        return new Object[] {
                new TestParameters(query, error)
        };
    }

    private String getQuery(String data) {
        String cData = null;
        {
            Pattern challenge = Pattern.compile(" data-challenge=\"([a-zA-Z0-9]+)\"");
            Matcher m = challenge.matcher(data);
            if (m.find()) {
                cData = m.group(1);
            }
        }
        try {
            byte[] privKeyData = priv.getEncoded();
            String privKey = URLEncoder.encode(PEM.encode("PRIVATE KEY", privKeyData), "UTF-8");
            privKeyData[0]++;
            String privKeyTampered = URLEncoder.encode(PEM.encode("PRIVATE KEY", privKeyData), "UTF-8");
            byte[] tampered = cert.cert().getEncoded();
            tampered[0]++;
            String query = pm.getQuery();
            query = query.replace("%serial", serial.toLowerCase())//
                    .replace("%Serial", serial.toUpperCase())//
                    .replace("%priv", privKey)//
                    .replace("%cert", URLEncoder.encode(PEM.encode("CERTIFICATE", cert.cert().getEncoded()), "UTF-8"))//
                    .replace("%tamperedCert", URLEncoder.encode(PEM.encode("CERTIFICATE", tampered), "UTF-8"))//
                    .replace("%tamperedPriv", privKeyTampered);
            if (cData != null) {
                byte[] sigRaw = KeyCompromiseForm.sign(priv, cData);
                String sigData = URLEncoder.encode(Base64.getEncoder().encodeToString(sigRaw), "UTF-8");
                sigRaw[0]++;
                query = query.replace("%signature", sigData);
                query = query.replace("%tamperedSignature", URLEncoder.encode(Base64.getEncoder().encodeToString(sigRaw), "UTF-8"));
            }
            return query;
        } catch (IOException e) {
            throw new Error(e);
        } catch (GeneralSecurityException e) {
            throw new Error(e);
        }
    }

    @Test
    public void testExecution() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        URLConnection uc = new URL("https://" + getServerName() + KeyCompromisePage.PATH).openConnection();
        String cookie = stripCookie(uc.getHeaderField("Set-Cookie"));
        String content = IOUtils.readURL(uc);
        String csrf = getCSRF(0, content);

        uc = new URL("https://" + getServerName() + KeyCompromisePage.PATH).openConnection();
        cookie(uc, cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + getQuery(content)//
        ).getBytes("UTF-8"));
        os.flush();
        HttpURLConnection huc = (HttpURLConnection) uc;

        String result = IOUtils.readURL(huc);
        String error = pm.getError();
        if (error == null) {
            assertThat(result, hasNoError());
            assertRevoked(result);
        } else if ("error".equals(error)) {
            assertThat(result, hasError());
            assertNotEquals(CertificateStatus.REVOKED, cert.getStatus());
        } else {
            assertThat(fetchStartErrorMessage(result), CoreMatchers.containsString(error));
            assertNotEquals(CertificateStatus.REVOKED, cert.getStatus());
        }
    }

    private void assertRevoked(String result) {
        assertThat(result, CoreMatchers.containsString("Certificate is revoked"));
        assertEquals(CertificateStatus.REVOKED, cert.getStatus());
    }
}
