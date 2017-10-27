package club.wpia.gigi.crypto.key;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;

public class KeyCheckPublicKeyFormatTest {

    @Test
    public void testFormatRSA() throws GeneralSecurityException, IOException {

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
            KeyCheck c = new KeyCheckPublicKeyFormat();
            c.check(pk);
        } catch (GigiApiException gae) {
            throw new Error("Valid key (RSA Public Key) rejected.", gae);
        }

    }

    @Test
    public void testFormatDSA() throws GeneralSecurityException, IOException {

        // DSA (using OpenSSH)
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MIIBtzCCASsGByqGSM44BAEwggEeAoGBAJpcf099rROPSjbJ5KWk5RF1ngRqXSo7\n" + //
                "cmKin9QPxIg0tXmxMGVS2sdtXYtmSJ9fewSAx0vHbojysEGY9ASXEoFpzDye4BbK\n" + //
                "yog9oHaUUEjxkSTwKcipu5BgM9b/nvigw/bs4dlEM+egdzf36lXXXJgvaTeXSpu9\n" + //
                "gKrKXTSi0jcvAhUAoH2Nbl6mRgAX4l6U5EXeg0zts3MCgYAW16cPIxLzmvrajRVR\n" + //
                "aIzAWpN1ApE/kx4CbtWZCdNttHu3c8D6qSnVrWpxY6FzrpeFniwg4vu73Ykh3crH\n" + //
                "0rVa20lrdRUAYGzbgInS+GLoPDGu1LukF0evJYZwyt6qsaFkQ54h4StSK+oM/mOi\n" + //
                "haLI45Rvlmade3KRQ/7YkV7DZQOBhQACgYEAjVGvOHImKynxgBl+eHeN2Ddqgj1+\n" + //
                "AKEOFKuFuedG9tKHtZXx04j982kaDnNc5cZY1KPFPYlS7jVJwcFPuf9Hi1/Aqq+3\n" + //
                "GnqaPl+tJtSpY2Chu8iIHIi5OXiwQC9ImtIEASZkkO+RIPLpzgb3GTn306NtMxae\n" + //
                "e+mhIx1IrbzMxSA=\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckPublicKeyFormat();
            c.check(pk);
        } catch (GigiApiException gae) {
            throw new Error("Valid key (DSA Public Key) rejected.", gae);
        }

    }

    @Test
    public void testFormatECDSA() throws GeneralSecurityException, IOException {

        // ECDSA (secp256r1 / P-256)
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEIQeJlVJLBpevYZjGWPPkD6hSrUEI\n" + //
                "G86i9e2p2QGCanQzNNM8Dkqv5Oa13qjxhRZNo2w+lVOBkAZyAptNKKT5Kw==\n" + //
                "-----END PUBLIC KEY-----\n";

        PublicKey pk = KeyCheckTest.pkFromString(sfk);
        try {
            KeyCheck c = new KeyCheckPublicKeyFormat();
            c.check(pk);
        } catch (GigiApiException gae) {
            throw new Error("Valid key (ECDSA Public Key on P-256) rejected.", gae);
        }

    }

    @Test
    public void testFormatGOST() throws GeneralSecurityException, IOException {

        // GOST R 34.10-2001 (256 bits)
        // https://lib.void.so/an-example-of-using-openssl-gost-engine-in-cc/
        String sfk = "-----BEGIN PUBLIC KEY-----\n" + //
                "MGMwHAYGKoUDAgITMBIGByqFAwICIwEGByqFAwICHgEDQwAEQDv/qpUxeRWXnyF8\n" + //
                "YwSUq7qQsL6MtD42GxLxqzLGx3NmpD4rHRay4xgQp91oTtqJjnybsplij0haRq7i\n" + //
                "Nf7QEdY=\n" + //
                "-----END PUBLIC KEY-----";

        final PublicKey pk;
        try {
            pk = KeyCheckTest.pkFromString(sfk);
        } catch (GeneralSecurityException gse) {
            assumeTrue("Could not load the GOST key due to lack of support", false);
            return;
        }

        try {
            KeyCheck c = new KeyCheckPublicKeyFormat();
            c.check(pk);
            fail("Unsupported key (GOST Public Key) accepted.");
        } catch (GigiApiException gae) {
            // expected
        }

    }

}
