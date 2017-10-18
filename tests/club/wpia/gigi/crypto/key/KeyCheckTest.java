package club.wpia.gigi.crypto.key;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import org.junit.Test;

import sun.security.util.DerValue;
import sun.security.x509.X509Key;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.util.PEM;

public class KeyCheckTest {

    public static PublicKey pkFromString(String pub) throws GeneralSecurityException, IOException {
        byte[] data = PEM.decode("PUBLIC KEY", pub);
        DerValue der = new DerValue(data);
        PublicKey key = X509Key.parse(der);

        return key;
    }

    @Test
    public void testNullKey() {
        try {
            KeyCheck.checkKey(null);
            fail("Providing a null key should fail!");
        } catch (GigiApiException gae) {
            // Expected failure
        }

        // Check that at least one key check has been loaded
        assertFalse(KeyCheck.getChecks().isEmpty());
    }

}
