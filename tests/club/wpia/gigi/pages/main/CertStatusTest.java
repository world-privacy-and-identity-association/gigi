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
import java.time.LocalDate;
import java.time.ZoneId;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.PEM;

public class CertStatusTest extends ClientTest {

    private Certificate cert;

    private Certificate certExpired;

    private String serial;

    private String serialExpired;

    private String foreignPEM = "-----BEGIN CERTIFICATE-----\n" + "MIIGvjCCBKagAwIBAgIVEQAAAAfLkxaJ4wATnrSBUbEr3UsxMA0GCSqGSIb3DQEB\n" + "DQUAMHExFzAVBgNVBAMMDkFzc3VyZWQgMjAxNy0yMSowKAYDVQQKDCFUZXN0IEVu\n" + "dmlyb25tZW50IENBIEx0ZC4tMTctMDMtMDQxHTAbBgNVBAsMFFRlc3QgRW52aXJv\n" + "bm1lbnQgQ0FzMQswCQYDVQQGEwJBVTAeFw0xNzA4MTUxMDI5NTdaFw0xNzA4MTYw\n" + "MDAwMDBaMDkxETAPBgNVBAMMCE1hcmN1cyBNMSQwIgYJKoZIhvcNAQkBFhVtLm1h\n" + "ZW5nZWxAaW5vcGlhZS5jb20wggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoIC\n" + "AQCv9hFCn69zHNapLimr4B9xK2PcYYRmINbBiihJ42WSIcf6VfxgQRPXZ9JCGDKn\n" + "haANqAyfOCuvtIuN1jJoYOo1VTQd3tkL9IvAwPVpsPiiHeYKqJRNxCkfU6kPGY2x\n" + "QV4+gDErXp/0AL792oAq6W3RoYIeiHXLKvLoYLBbSqtTCkfCYgEhv/3bflswU1JH\n" + "fr6QsvUJ1epH7QpiE5J8pp9hWKfcEufekYnMWASKITS4ronQcyfMocf9BlEf87ou\n" + "wri0NF8EBBhwq6C2+Ag9QlNHtylyUTj4+3XR//3K+6T/8neNK/9CNZ6sXqz5SnFX\n" + "BZTQONK2vavDvbSDhgQ0CuCbyN+rwjjSHYSgywqjkKb1tzB39N7Hd2fR5LcnBD3/\n" + "alQGIh808iukSm7TNwmdSCl2dRug2nTH5qdFLgk2wH+UcoOZH1lEn3UA2IYScmUH\n" + "sgeF6bIojS8Qj1UQZPwlblDiNvudYx2QQG9aNqWz+4O+6a5IpRugY9jnG5Z5sPum\n" + "IpXl1q+VNz8FLlZavpxccjGlIW0179kctA5FEoTHgogzE/rAt5tmHD+kdVEgpquR\n" + "yjpVVYG/R64oUQDjBeen1aKt2yzv+CP1frvml/bUKcb4qZ3z15K6gD0wrKQVWJoD\n" + "0j6gPAs10N2khPbjX9sYJqFr4Tket1DtCIusPQj7JxQm1wIDAQABo4IBgzCCAX8w\n" + "DAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQU5N/6GJVVMyrAd/HgiN7PQQ7mTOUwHwYD\n" + "VR0jBBgwFoAUwygt1+5B0HactieygKVNyE3m9W0wDgYDVR0PAQH/BAQDAgOoMB0G\n" + "A1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDBDCBjgYIKwYBBQUHAQEEgYEwfzAz\n" + "BggrBgEFBQcwAYYnaHR0cDovL2cyLm9jc3AudGVzdDEuYmFja3VwLmRvZ2NyYWZ0\n" + "LmRlMEgGCCsGAQUFBzAChjxodHRwOi8vZzIuY3J0LnRlc3QxLmJhY2t1cC5kb2dj\n" + "cmFmdC5kZS9nMi8yMDE3L2Fzc3VyZWQtMi5jcnQwTQYDVR0fBEYwRDBCoECgPoY8\n" + "aHR0cDovL2cyLmNybC50ZXN0MS5iYWNrdXAuZG9nY3JhZnQuZGUvZzIvMjAxNy9h\n" + "c3N1cmVkLTIuY3JsMCAGA1UdEQQZMBeBFW0ubWFlbmdlbEBpbm9waWFlLmNvbTAN\n" + "BgkqhkiG9w0BAQ0FAAOCAgEATRC7wwfFNExFk6LGcAbYSJViVs8ZgFuaTEzlBrik\n" + "mf9f8QA7Aj2bH2hqCdjbem1ElXhbADcJopS46P7yfH57zUj3qvD9znK0DdqWpvsO\n" + "nCB7/kdA0KysxTZ+D5gFgk/MpDfNP8ALB1SHGEOv/l4gQs0Zn6ORxt+4zrLzqExO\n" + "dMYdxcVQCl0ft5tQRUSxg1k2y8crgplR02TvhJCrb+RNCS0SQMkEA11bZKEpLBYk\n" + "bJMJYMr+SMN/wtC/vjXm9hrPGqnfqpJC7IqHUfzcBt10dGPqzvO/6xnEZn4YSgjr\n" + "MyoVUnOmcgolFrToYbXr3CNoQFO5Dgz7hbXH59/6ph35g7Q3hllTV+DGV753Baaa\n" + "bMgAsUeJqdMcJSAorLKjibinF/odbJ/kghAg7LBLQUmCvfYWzKhnfETXQ/qXbOk7\n" + "fufEB0z1AnzOB032Cde+FZg1NofjyF8N0UuK4l8fS+hSX6bcJaIuvUSNm5Mj2laZ\n" + "cskPgOu2Gng1JteLbotEKnruKshfKgo64Fq/mPASHfrSdAeQ/shlL6JG3QQeiw9k\n" + "Yu7lu7neRduthxwEdZ8EYrQ0fnHWrmnGsDCpvNIv1coaPc0ghi2pfGjEBAXGQoQ3\n" + "7Bia1anze/wG/9viZyuH1Ms10Ya9E8bPfB1D7B26tB6IZUNLaMnoYbCd+EN7Zjx/\n" + "Yac=\n" + "-----END CERTIFICATE-----";

    public CertStatusTest() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {

        KeyPair kp = generateKeypair();
        String csr = generatePEMCSR(kp, "CN=test");
        CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), csr);
        cr.update(CertificateRequest.DEFAULT_CN, Digest.SHA512.toString(), "client", null, null, "email:" + email + "\n");
        cert = cr.draft();
        Job j = cert.issue(null, "2y", u);
        await(j);
        serial = cert.getSerial().toLowerCase();

        certExpired = cr.draft();
        j = certExpired.issue(null, "2y", u);
        await(j);
        serialExpired = certExpired.getSerial().toLowerCase();
        try (GigiPreparedStatement prep = new GigiPreparedStatement("UPDATE `certs` SET `expire`=?  WHERE `id`=?")) {
            prep.setDate(1, java.sql.Date.valueOf(LocalDate.now(ZoneId.of("UTC"))));
            prep.setInt(2, certExpired.getId());
            prep.execute();
        }

    }

    @Test
    public void testCertStatus() throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        testExecution("serial=" + URLEncoder.encode(serial, "UTF-8"), null, false, false);// serial
        testExecution("serial=0000" + URLEncoder.encode(serial, "UTF-8"), null, false, false);// leading
        // Zeros
        testExecution("serial=0000" + URLEncoder.encode(serial.toUpperCase(), "UTF-8"), null, false, false);// upper
        // case

        testExecution("serial=0000", "Malformed serial", false, false);
        testExecution("serial=0lkd", "Malformed serial", false, false);

        testExecution("cert=" + URLEncoder.encode(PEM.encode("CERTIFICATE", cert.cert().getEncoded()), "UTF-8"), null, false, false);
        testExecution("cert=" + URLEncoder.encode(foreignPEM, "UTF-8"), "Certificate to check not found.", false, false);
        testExecution("cert=sometext", "Certificate could not be parsed", false, false);

        await(cert.revoke(RevocationType.USER));

        testExecution("serial=" + URLEncoder.encode(serial, "UTF-8"), "Certificate has been revoked on ", true, false);// serial
        testExecution("cert=" + URLEncoder.encode(PEM.encode("CERTIFICATE", cert.cert().getEncoded()), "UTF-8"), "Certificate has been revoked on ", true, false);

        testExecution("serial=" + URLEncoder.encode(serialExpired, "UTF-8"), null, false, true);// serial
        testExecution("cert=" + URLEncoder.encode(PEM.encode("CERTIFICATE", certExpired.cert().getEncoded()), "UTF-8"), null, false, true);

    }

    public void testExecution(String query, String error, boolean revoked, boolean expired) throws IOException, InterruptedException, GigiApiException, GeneralSecurityException {
        URLConnection uc = new URL("https://" + getServerName() + CertStatusRequestPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String content = IOUtils.readURL(uc);
        String csrf = getCSRF(0, content);

        uc = new URL("https://" + getServerName() + CertStatusRequestPage.PATH).openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" + query).getBytes("UTF-8"));
        os.flush();
        HttpURLConnection huc = (HttpURLConnection) uc;

        String result = IOUtils.readURL(huc);

        if (error == null) {
            assertThat(result, hasNoError());
            if (expired) {
                assertThat(result, CoreMatchers.containsString("Certificate is valid but has expired on"));
            } else {
                assertThat(result, CoreMatchers.containsString("Certificate is valid."));
            }
        } else {
            assertThat(fetchStartErrorMessage(result), CoreMatchers.containsString(error));
            if (revoked == false) {
                assertNotEquals(CertificateStatus.REVOKED, cert.getStatus());
            } else {
                assertEquals(CertificateStatus.REVOKED, cert.getStatus());
            }
        }
    }
}
