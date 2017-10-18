package club.wpia.gigi.crypto.key;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;

// Vulnerable keys for this test taken from
// @link https://misissued.com/batch/28/
public class KeyCheckROCATest {

    @Test
    public void testROCASaneKey() throws GeneralSecurityException, IOException {

        // Normal public key generated with OpenSSL:
        // openssl genpkey -algorithm rsa -pkeyopt rsa_keygen_bits:2048
        // -pkeyopt rsa_keygen_pubexp:7331 2>/dev/null |
        // openssl pkey -pubout -outform pem
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBITANBgkqhkiG9w0BAQEFAAOCAQ4AMIIBCQKCAQEArcAPmy3RnXdwyFg3V9k1\n" + //
                "RaFR/peHa3hLsmh25BInRVArbaMctSBaJBVZwQIgBdqjyITQQZP38i6k+WdsETn9\n" + //
                "J491UDLKU3E3UG60ZS3BzcJllNdpn4g0IZROxmmUz2JlAXkGtIglmWWDx14qHSNj\n" + //
                "ON58mc3ihfn/oWkPk2hk/csDxGQq5jSaBUwa9THBg9UQHHBqQbhp2nGfa5a5VRlI\n" + //
                "0QeIy+8GmKlXYMchReUI25ksLOzaqETD0UXiAPyt+vpvkKCDjWGc3kjabn6OkuTt\n" + //
                "na7N/52qrEC2ImuanYlzR5gv9jkbFF2PiMIEBD+3B0842rLx0X/lbXhRr1MtuHtN\n" + //
                "tQICHKM=\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckROCA();
            c.check(pk);
        } catch (GigiApiException gae) {
            throw new Error("Valid key (not vulnerable to ROCA vulnerability) rejected.", gae);
        }

    }

    @Test
    public void testROCAVulnerable1() throws GeneralSecurityException, IOException {

        // D-TRUST Qualified Root CA 1 2014:PN
        // https://crt.sh/?id=26311918&opt=cablint
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBJDANBgkqhkiG9w0BAQEFAAOCAREAMIIBDAKCAQEAlT2Gi8cR+hX+0iYaYH0e\n" + //
                "Pmxrqq1tNKlvcesp1wwIeixqeQ2/QJkFMEAVq3hX45Cri7Z/p9ch8+Nd7eva80Ym\n" + //
                "nn0llfQ2kJDhi1fOTfodR7IN24105y5D6Lf3zre6J2FOxqPH/q0dDJAbTbuaO4kS\n" + //
                "yI9xUEhvHo8oZ0L3SGq6VyeeOBXDoBg4xp6xp1w6cZ76/3HhuBc26sgoO9AvDRzp\n" + //
                "M74wvzGBSVaA8+SU1O46plY4os4GlHEdcZM/0NcHeiWwJvycPKkurVL9AxDBq9Iw\n" + //
                "Dox/+zQzxcS7txvrJeI1ahQwPpzYdJEwFQ6/rCt43KALWt+OoAIvW5TVYllaF62Z\n" + //
                "XwIFAJLK1sU=\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckROCA();
            c.check(pk);
            fail("Invalid key (ROCA vulnerable) accepted.");
        } catch (GigiApiException gae) {
            // expected
        }

    }

    @Test
    public void testROCAVulnerable2() throws GeneralSecurityException, IOException {

        // D-TRUST Qualified Root CA 2 2014:PN
        // https://crt.sh/?id=26310640&opt=cablint
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBJDANBgkqhkiG9w0BAQEFAAOCAREAMIIBDAKCAQEAmDbSRazHfc1YoqH6dXWz\n" + //
                "k2zBJadliqHgpft1Z5HqXF6AzXQ8duHLN3Db+SSDUWP+fDv1Ti69wmH5HqrdSGcl\n" + //
                "EvoNStTRjFpnzj/7c5AkALWeZlRzcrBjeIFTtSdZvgluA14BnQXmRViC3tgOFMyU\n" + //
                "I72wqCGuf7Y8cW/DSfSzBWFTO+A9uoj0oMKEaaLd1iVF4mctKf/atrHzy3Ny1/d9\n" + //
                "WgbLLxiGtrNxVh78j9HCS4rs17AEC3OZnosUE3jCzLCHyQjwI+frkmINj5Qy4L3j\n" + //
                "GJqxtIBBb9LwaCkkuV3g679/V4BhWKpDt6YIo/YYINRu42GhXSB9x13KhSMGe9vn\n" + //
                "eQIFAKY6EqM=\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckROCA();
            c.check(pk);
            fail("Invalid key (ROCA vulnerable) accepted.");
        } catch (GigiApiException gae) {
            // expected
        }

    }

    @Test
    public void testROCAVulnerable3() throws GeneralSecurityException, IOException {

        // D-TRUST Qualified Root CA 3 2014:PN
        // https://crt.sh/?id=26310642&opt=cablint
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBJDANBgkqhkiG9w0BAQEFAAOCAREAMIIBDAKCAQEAlpwnRwC1ogIM/Wywu3ys\n" + //
                "HhREKeT56eDAMO+68dvz/mWL7dzFhIFHdehRpSpICx06tb7YpK6/XX9/0okTKajt\n" + //
                "K0paM3mqZWNilpZnCzItFjwYjxKZL8Bgxww0ztqGD/2oHtmviZNO6yeaLYmm2Eqv\n" + //
                "hXCVPUCcE17BPjybSZaW3ULaTiIQFYcCB5/utyXu3RT8ss2NBNoD9D4S5r3dMMJY\n" + //
                "qUE/oojbg/4Y955M0S+yEUuv2dfbE+BCkZqgM05yk/wNr9L8F2f7cG2h/qjFUBE5\n" + //
                "91kZXZ0g3lBhbKx9SUM8/Vq3WMmfDDpV2qk9wXC0sMgVAwTYLN1J3LWow/C+4Ffo\n" + //
                "xQIFAI0kKjs=\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckROCA();
            c.check(pk);
            fail("Invalid key (ROCA vulnerable) accepted.");
        } catch (GigiApiException gae) {
            // expected
        }

    }

}
