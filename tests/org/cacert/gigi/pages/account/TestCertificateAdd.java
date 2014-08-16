package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.Digest;
import org.cacert.gigi.User;
import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.PEM;
import org.junit.Test;
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

public class TestCertificateAdd extends ManagedTest {

    KeyPair kp = generateKeypair();

    User u = User.getById(createVerifiedUser("testuser", "testname", uniq + "@testdom.com", TEST_PASSWORD));

    String session = login(uniq + "@testdom.com", TEST_PASSWORD);

    String csrf;

    public TestCertificateAdd() throws GeneralSecurityException, IOException {
        TestDomain.addDomain(session, uniq + ".tld");

    }

    @Test
    public void testSimpleServer() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
            CertificateIssueForm.OID_KEY_USAGE_SSL_SERVER
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
            CertificateIssueForm.OID_KEY_USAGE_EMAIL_PROTECTION
        }, new DNSName("a." + uniq + ".tld"), new DNSName("b." + uniq + ".tld"), new RFC822Name(uniq + "@testdom.com"));

        String pem = generatePEMCSR(kp, "CN=testuser testname", atts, "SHA384WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "mail", "testuser testname", "dns:a." + uniq + ".tld\ndns:b." + uniq + ".tld\nemail:" + uniq + "@testdom.com\n", Digest.SHA384.toString()
        }, res);
    }

    @Test
    public void testSimpleClient() throws IOException, GeneralSecurityException {
        PKCS10Attributes atts = buildAtts(new ObjectIdentifier[] {
            CertificateIssueForm.OID_KEY_USAGE_SSL_CLIENT
        }, new RFC822Name(uniq + "@testdom.com"));

        String pem = generatePEMCSR(kp, "CN=testuser testname,email=" + uniq + "@testdom.com", atts, "SHA512WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "client", "testuser testname", "email:" + uniq + "@testdom.com\n", Digest.SHA512.toString()
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
            CertificateIssueForm.OID_KEY_USAGE_SSL_CLIENT
        }, new RFC822Name(uniq + "@testdom.com"));

        String pem = generatePEMCSR(kp, "CN=testuser testname,email=" + uniq + "@testdom.com", atts, "SHA512WithRSA");

        String[] res = fillOutForm("CSR=" + URLEncoder.encode(pem, "UTF-8"));
        assertArrayEquals(new String[] {
                "client", "testuser testname", "email:" + uniq + "@testdom.com\n", Digest.SHA512.toString()
        }, res);

        HttpURLConnection huc = (HttpURLConnection) ncert.openConnection();
        huc.setRequestProperty("Cookie", session);
        huc.setDoOutput(true);
        OutputStream out = huc.getOutputStream();
        out.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8")).getBytes());
        out.write(("&profile=client&CN=testuser+testname&SANs=" + URLEncoder.encode("email:" + uniq + "@testdom.com\n", "UTF-8")).getBytes());
        out.write(("&hash_alg=SHA512&CCA=y").getBytes());
        URLConnection uc = authenticate(new URL(huc.getHeaderField("Location") + ".crt"));
        String crt = IOUtils.readURL(new InputStreamReader(uc.getInputStream(), "UTF-8"));

        uc = authenticate(new URL(huc.getHeaderField("Location") + ".cer"));
        byte[] cer = IOUtils.readURL(uc.getInputStream());
        assertArrayEquals(cer, PEM.decode("CERTIFICATE", crt));

        uc = authenticate(new URL(huc.getHeaderField("Location") + ".cer?install"));
        byte[] cer2 = IOUtils.readURL(uc.getInputStream());
        assertArrayEquals(cer, cer2);
        assertEquals("application/x-x509-user-cert", uc.getHeaderField("Content-type"));

        uc = authenticate(new URL(huc.getHeaderField("Location")));
        String gui = IOUtils.readURL(uc);
        assertThat(gui, containsString("clientAuth"));
        assertThat(gui, containsString("CN=testuser testname"));
        assertThat(gui, containsString("SHA512withRSA"));
        assertThat(gui, containsString("RFC822Name: " + uniq + "@testdom.com"));

    }

    private URLConnection authenticate(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Cookie", session);
        return uc;
    }

    protected String testSPKAC(boolean correctChallange) throws GeneralSecurityException, IOException {
        HttpURLConnection uc = (HttpURLConnection) ncert.openConnection();
        uc.setRequestProperty("Cookie", session);
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
                    "client", CertificateIssueForm.DEFAULT_CN, "", Digest.SHA512.toString()
            }, res);
        } catch (Error e) {
            assertTrue(e.getMessage().startsWith("<div>Challenge mismatch"));
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
        uc.setRequestProperty("Cookie", session);
        csrf = getCSRF(uc);
        return fillOutFormDirect(pem);

    }

    private String[] fillOutFormDirect(String pem) throws IOException {

        HttpURLConnection uc = (HttpURLConnection) ncert.openConnection();
        uc.setRequestProperty("Cookie", session);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" + pem).getBytes());
        uc.getOutputStream().flush();

        return extractFormData(uc);
    }

    private String[] extractFormData(HttpURLConnection uc) throws IOException, Error {
        String result = IOUtils.readURL(uc);
        if (result.contains("<div class='formError'>")) {
            String s = fetchStartErrorMessage(result);
            throw new Error(s);
        }

        String profileKey = extractPattern(result, Pattern.compile("<option value=\"([^\"]*)\" selected>"));
        String resultingCN = extractPattern(result, Pattern.compile("<input [^>]*name='CN' [^>]*value='([^']*)'>"));
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
