package club.wpia.gigi.crypto;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAKey;
import java.util.Base64;

import org.junit.Test;

import club.wpia.gigi.crypto.SPKAC;
import club.wpia.gigi.testUtils.IOUtils;
import sun.security.x509.X509Key;

public class TestSPKAC {

    @Test
    public void testParse() throws GeneralSecurityException, IOException {
        String spkac = IOUtils.readURL(new InputStreamReader(TestSPKAC.class.getResourceAsStream("sampleSPKAC.txt"), "UTF-8"));
        SPKAC parsed = new SPKAC(Base64.getDecoder().decode(spkac.replaceAll("[\r\n]", "")));
        assertEquals("i am in the testcase", parsed.getChallenge());
        RSAKey k = ((RSAKey) parsed.getPubkey());
        assertEquals("a4004c2addf204fb26ce98f5963cc76def609ec0c50905e091fb84e986e3cb" + //
                "0d5e14edb9cb8e10524350bd2351589284a4f631ddf9b87f04ea0e58f7d8d816b58" + //
                "d052ce08b6576c04a7d45daf25b0ac9306f9cbb1f626e4ac301b7a4a3a062252b9a" + //
                "472b2cde5ec803407b18879a59ccba7716016b1de4537a005b2bd0fd6071", k.getModulus().toString(16));
    }

    @Test
    public void testAddData() throws GeneralSecurityException, IOException {
        String spkac = IOUtils.readURL(new InputStreamReader(TestSPKAC.class.getResourceAsStream("sampleSPKAC.txt"), "UTF-8"));
        byte[] data = Base64.getDecoder().decode(spkac.replaceAll("[\r\n]", ""));
        byte[] tampered = new byte[data.length + 1];
        System.arraycopy(data, 0, tampered, 0, data.length);
        try {
            new SPKAC(tampered);
            fail("Expected illegal arg exception.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        // change the last byte of the signature.
        data[data.length - 1]--;
        try {
            new SPKAC(data);
            fail("Expected SignatureException.");
        } catch (SignatureException e) {
            // expected
        }
    }

    @Test
    public void testGen() throws GeneralSecurityException, IOException {
        KeyPairGenerator pkg = KeyPairGenerator.getInstance("RSA");
        pkg.initialize(1024);
        KeyPair kp = pkg.generateKeyPair();

        SPKAC s = new SPKAC((X509Key) kp.getPublic(), "this is a even bigger challenge");
        Signature sign = Signature.getInstance("SHA512withRSA");
        sign.initSign(kp.getPrivate());

        byte[] res = s.getEncoded(sign);
        SPKAC parsed = new SPKAC(res);
        assertEquals(s.getChallenge(), parsed.getChallenge());
        assertEquals(s.getPubkey(), parsed.getPubkey());

    }
}
