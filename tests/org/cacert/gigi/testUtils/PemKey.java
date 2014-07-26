package org.cacert.gigi.testUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class PemKey {

    public static PrivateKey parsePEMPrivateKey(String privKeyPEM) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (privKeyPEM.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
            // key is pkcs1 convert to p8
            try {
                Process p = Runtime.getRuntime().exec(new String[] {
                        "openssl", "pkcs8", "-topk8", "-nocrypt"
                });
                p.getOutputStream().write(privKeyPEM.getBytes());
                p.getOutputStream().close();
                privKeyPEM = IOUtils.readURL(new InputStreamReader(p.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        privKeyPEM = privKeyPEM.replaceAll("-----BEGIN PRIVATE KEY-----", "").replace("\n", "");
        // Remove the first and last lines
        privKeyPEM = privKeyPEM.replaceAll("-----END PRIVATE KEY-----", "");
        // Base64 decode the data
        byte[] encoded = Base64.getDecoder().decode(privKeyPEM);

        // PKCS8 decode the encoded RSA private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);
        return privKey;
    }
}
