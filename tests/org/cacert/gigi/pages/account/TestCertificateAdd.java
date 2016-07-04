package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.pages.account.certs.CertificateAdd;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.util.PEM;
import org.junit.Test;

import sun.security.pkcs.PKCS7;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.DNSName;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.RFC822Name;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X509Key;

public class TestCertificateAdd extends ClientTest {

    private static class OnPageError extends Error {

        public OnPageError(String page) {
            super(page);
        }
    }

    KeyPair kp = generateKeypair();

    String csrf;

    public TestCertificateAdd() throws GeneralSecurityException, IOException {
        TestDomain.addDomain(cookie, uniq + ".tld");

    }

    @Test
    public void testSimpleServer() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
                CertificateRequest.OID_KEY_USAGE_SSL_SERVER
        }, new DNSName(uniq + ".tld"));

        String pem = generatePEMCSR(kp, "CN=a." + uniq + ".tld", atts);

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "server", "CAcert WoT User", "dns:a." + uniq + ".tld\ndns:" + uniq + ".tld\n", Digest.SHA256.toString()
        }, res);
    }

    @Test
    public void testSimpleMail() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
                CertificateRequest.OID_KEY_USAGE_EMAIL_PROTECTION
        }, new DNSName("a." + uniq + ".tld"), new DNSName("b." + uniq + ".tld"), new RFC822Name(email));

        String pem = generatePEMCSR(kp, "CN=a b", atts, "SHA384WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "mail", "a b", "email:" + email + "\ndns:a." + uniq + ".tld\ndns:b." + uniq + ".tld\n", Digest.SHA384.toString()
        }, res);
    }

    @Test
    public void testSimpleClient() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
                CertificateRequest.OID_KEY_USAGE_SSL_CLIENT
        }, new RFC822Name(email));

        String pem = generatePEMCSR(kp, "CN=a b,email=" + email, atts, "SHA512WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "client", "a b", "email:" + email + "\n", Digest.SHA512.toString()
        }, res);
    }

    @Test
    public void testSPKAC() throws GeneralSecurityException, IOException {
        testSPKAC(false);
        testSPKAC(true);
    }

    @Test
    public void testIssue() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
                CertificateRequest.OID_KEY_USAGE_SSL_CLIENT
        }, new RFC822Name(email));

        String pem = generatePEMCSR(kp, "CN=a b,email=" + email, atts, "SHA512WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "client", "a b", "email:" + email + "\n", Digest.SHA512.toString()
        }, res);

        HttpURLConnection huc = (HttpURLConnection) ncert.openConnection();
        huc.setRequestProperty("Cookie", cookie);
        huc.setDoOutput(true);
        OutputStream out = huc.getOutputStream();
        out.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8")).getBytes("UTF-8"));
        out.write(("&CN=CAcert+WoT+User&profile=client&SANs=" + URLEncoder.encode("email:" + email + "\n", "UTF-8")).getBytes("UTF-8"));
        out.write(("&hash_alg=SHA512&tos_agree=y").getBytes("UTF-8"));
        URLConnection uc = authenticate(new URL(huc.getHeaderField("Location") + ".crt"));
        String crt = IOUtils.readURL(new InputStreamReader(uc.getInputStream(), "UTF-8"));

        uc = authenticate(new URL(huc.getHeaderField("Location") + ".cer"));
        byte[] cer = IOUtils.readURL(uc.getInputStream());
        assertArrayEquals(cer, PEM.decode("CERTIFICATE", crt));

        uc = authenticate(new URL(huc.getHeaderField("Location") + ".cer?install&chain"));
        byte[] pkcs7 = IOUtils.readURL(uc.getInputStream());
        PKCS7 p7 = new PKCS7(pkcs7);
        byte[] sub = verifyChain(p7.getCertificates());
        assertArrayEquals(cer, sub);
        assertEquals("application/x-x509-user-cert", uc.getHeaderField("Content-type"));

        uc = authenticate(new URL(huc.getHeaderField("Location")));
        String gui = IOUtils.readURL(uc);
        Pattern p = Pattern.compile("-----BEGIN CERTIFICATE-----[^-]+-----END CERTIFICATE-----");
        Matcher m = p.matcher(gui);
        assertTrue(m.find());
        byte[] cert = PEM.decode("CERTIFICATE", m.group(0));
        Certificate c = CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(cert));
        gui = c.toString();
        assertThat(gui, containsString("clientAuth"));
        assertThat(gui, containsString("CN=CAcert WoT User"));
        assertThat(gui, containsString("SHA512withRSA"));
        assertThat(gui, containsString("RFC822Name: " + email));

    }

    private byte[] verifyChain(X509Certificate[] x509Certificates) throws GeneralSecurityException {
        X509Certificate current = null;
        nextCert:
        while (true) {
            for (int i = 0; i < x509Certificates.length; i++) {
                X509Certificate cert = x509Certificates[i];
                if (current == null) {
                    if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
                        current = cert;
                        continue nextCert;
                    }
                } else {
                    if (cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
                        continue;
                    }
                    if (current.getSubjectX500Principal().equals(cert.getIssuerX500Principal())) {
                        Signature s = Signature.getInstance(cert.getSigAlgName());
                        s.initVerify(current.getPublicKey());
                        s.update(cert.getTBSCertificate());
                        assertTrue(s.verify(cert.getSignature()));
                        current = cert;
                        continue nextCert;
                    }
                }
            }
            assertNotNull(current);
            return current.getEncoded();
        }
    }

    @Test
    public void testValidityPeriodCalendar() throws IOException, GeneralSecurityException {
        testCertificateValidityRelative(Calendar.YEAR, 2, "2y", true);
        testCertificateValidityRelative(Calendar.YEAR, 1, "1y", true);
        testCertificateValidityRelative(Calendar.MONTH, 3, "3m", true);
        testCertificateValidityRelative(Calendar.MONTH, 7, "7m", true);
        testCertificateValidityRelative(Calendar.MONTH, 13, "13m", true);

        testCertificateValidityRelative(Calendar.MONTH, 13, "-1m", false);
    }

    @Test
    public void testValidityPeriodWhishStart() throws IOException, GeneralSecurityException {
        long now = System.currentTimeMillis();
        final long MS_PER_DAY = 24 * 60 * 60 * 1000;
        now -= now % MS_PER_DAY;
        now += MS_PER_DAY;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date start = new Date(now);
        Date end = new Date(now + MS_PER_DAY * 10);
        String validity = "&validFrom=" + sdf.format(start) + "&validity=" + sdf.format(end);
        X509Certificate res = createCertWithValidity(validity);
        assertNotNull(validity, res);
        assertEquals(start, res.getNotBefore());
        assertEquals(end, res.getNotAfter());
    }

    private void testCertificateValidityRelative(int field, int amount, String length, boolean shouldsucceed) throws IOException, GeneralSecurityException, UnsupportedEncodingException, MalformedURLException, CertificateException {
        X509Certificate parsed = createCertWithValidity("&validFrom=now&validity=" + length);
        if (parsed == null) {
            assertTrue( !shouldsucceed);
            return;
        } else {
            assertTrue(shouldsucceed);
        }

        long now = System.currentTimeMillis();
        Date start = parsed.getNotBefore();
        Date end = parsed.getNotAfter();
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(start);
        c.add(field, amount);
        assertTrue(Math.abs(start.getTime() - now) < 10000);
        assertEquals(c.getTime(), end);
    }

    private X509Certificate createCertWithValidity(String validity) throws IOException, GeneralSecurityException, UnsupportedEncodingException, MalformedURLException, CertificateException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
                CertificateRequest.OID_KEY_USAGE_SSL_CLIENT
        }, new RFC822Name(email));

        String pem = generatePEMCSR(kp, "CN=a b", atts, "SHA512WithRSA");
        fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));

        HttpURLConnection huc = (HttpURLConnection) ncert.openConnection();
        huc.setRequestProperty("Cookie", cookie);
        huc.setDoOutput(true);
        OutputStream out = huc.getOutputStream();
        out.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8")).getBytes("UTF-8"));
        out.write(("&profile=client&CN=" + CertificateRequest.DEFAULT_CN + "&SANs=" + URLEncoder.encode("email:" + email + "\n", "UTF-8")).getBytes("UTF-8"));
        out.write(("&hash_alg=SHA512&tos_agree=y&").getBytes("UTF-8"));
        out.write(validity.getBytes("UTF-8"));

        String certurl = huc.getHeaderField("Location");
        if (certurl == null) {
            return null;
        }
        URLConnection uc = authenticate(new URL(certurl + ".crt"));
        String crt = IOUtils.readURL(new InputStreamReader(uc.getInputStream(), "UTF-8"));

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate parsed = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(crt.getBytes("UTF-8")));
        return parsed;
    }

    private URLConnection authenticate(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Cookie", cookie);
        return uc;
    }

    protected String testSPKAC(boolean correctChallange) throws GeneralSecurityException, IOException {
        HttpURLConnection uc = (HttpURLConnection) ncert.openConnection();
        uc.setRequestProperty("Cookie", cookie);
        String s = IOUtils.readURL(uc);

        csrf = extractPattern(s, Pattern.compile("<input [^>]*name='csrf' [^>]*value='([^']*)'>"));
        String challenge = extractPattern(s, Pattern.compile("<keygen [^>]*name=\"SPKAC\" [^>]*challenge=\"([^\"]*)\"/>"));

        SPKAC spk = new SPKAC((X509Key) kp.getPublic(), challenge + (correctChallange ? "" : "b"));
        Signature sign = Signature.getInstance("SHA512WithRSA");
        sign.initSign(kp.getPrivate());
        try {
            String[] res = fillOutFormDirect("SPKAC=" + URLEncoder.encode(Base64.getEncoder().encodeToString(spk.getEncoded(sign)), "UTF-8"));
            if ( !correctChallange) {
                fail("Should not succeed with wrong challange.");
            }
            assertArrayEquals(new String[] {
                    "client", CertificateRequest.DEFAULT_CN, "", Digest.SHA512.toString()
            }, res);
        } catch (OnPageError e) {
            String error = fetchStartErrorMessage(e.getMessage());
            assertTrue(error, error.startsWith("<p>Challenge mismatch"));
        }
        return csrf;
    }

    private PKCS10Attributes buildAtts(ObjectIdentifier[] ekuOIDs, GeneralNameInterface... SANs) throws IOException {
        CertificateExtensions attributeValue = new CertificateExtensions();
        GeneralNames names = new GeneralNames();

        for (GeneralNameInterface name : SANs) {
            names.add(new GeneralName(name));
        }
        attributeValue.set("SANs", new SubjectAlternativeNameExtension(names));
        PKCS10Attributes atts = new PKCS10Attributes(new PKCS10Attribute[] {
                new PKCS10Attribute(PKCS9Attribute.EXTENSION_REQUEST_OID, attributeValue)
        });
        ExtendedKeyUsageExtension eku = new ExtendedKeyUsageExtension(//
                new Vector<>(Arrays.<ObjectIdentifier>asList(ekuOIDs)));
        attributeValue.set("eku", eku);
        return atts;
    }

    private final URL ncert = new URL("https://" + getServerName() + CertificateAdd.PATH);

    private String[] fillOutForm(String pem) throws IOException {
        HttpURLConnection uc = (HttpURLConnection) ncert.openConnection();
        uc.setRequestProperty("Cookie", cookie);
        csrf = getCSRF(uc);
        return fillOutFormDirect(pem);

    }

    private String[] fillOutFormDirect(String pem) throws IOException {

        HttpURLConnection uc = (HttpURLConnection) ncert.openConnection();
        uc.setRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" + pem).getBytes("UTF-8"));
        uc.getOutputStream().flush();

        return extractFormData(uc);
    }

    private String[] extractFormData(HttpURLConnection uc) throws IOException, Error {
        String result = IOUtils.readURL(uc);
        if (hasError().matches(result)) {
            throw new OnPageError(result);
        }

        String profileKey = extractPattern(result, Pattern.compile("<option value=\"([^\"]*)\" selected>"));
        String resultingCN = extractPattern(result, Pattern.compile("<input [^>]*name='CN' [^>]*value='([^']*)'/>"));
        String txt = extractPattern(result, Pattern.compile("<textarea [^>]*name='SANs' [^>]*>([^<]*)</textarea>"));
        String md = extractPattern(result, Pattern.compile("<input type=\"radio\" [^>]*name=\"hash_alg\" value=\"([^\"]*)\" checked='checked'/>"));
        return new String[] {
                profileKey, resultingCN, txt, md
        };
    }

    private String extractPattern(String result, Pattern p) {
        Matcher m = p.matcher(result);
        assertTrue(m.find());
        String resultingCN = m.group(1);
        return resultingCN;
    }
}
