package club.wpia.gigi.crypto.key;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;

public class KeyCheckSmallFactorsTest {

    @Test
    public void testSmallPrimesSaneKey() throws GeneralSecurityException, IOException {

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
            KeyCheck c = new KeyCheckSmallFactors();
            c.check(pk);
        } catch (GigiApiException gae) {
            throw new Error("Valid key (regarding small factors) rejected.", gae);
        }

    }

    @Test
    public void testSmallPrimes() throws GeneralSecurityException, IOException {

        // The following key is the above one multiplied by 7331.
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQITb6L+6NEZsFNiuTY42LEg\n" + //
                "iPqvDa1K+pXftgWEpTPalebLpKX/Ft11V09pQh/bB6QgNzNXxfBVXE2+UhyrsU+c\n" + //
                "g+Esd55384MjBFI37W1U50Xi9VS1s3ls3ZoL2+GAbs6yeSzLA9bMt8YMtj2QAGxi\n" + //
                "ZYtKKHLd4qYja0OZCkaED8ys4QB4flRWpbJn+4/Yoj5sXmcy2AP/SoPIRf09T/MQ\n" + //
                "OerCZ/3p5blhOGZt1I3MqJNcCoK5oKkzkeQ3AkPqOjGo2qSXObJPMYBHKjIs2JHA\n" + //
                "ioTVHwAOgsEfu69srVcgOzsleAVSeDNFWUv5BayWVlGpHtJi4mGHDdyLL7r2SfMC\n" + //
                "Rj8CAhyj\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckSmallFactors();
            c.check(pk);
            fail("Invalid key (containing small factors) accepted.");
        } catch (GigiApiException gae) {
            // expected
        }

    }

}
