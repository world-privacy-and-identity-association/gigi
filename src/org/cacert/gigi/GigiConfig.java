package org.cacert.gigi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

public class GigiConfig {

    public static final String GIGI_CONFIG_VERSION = "GigiConfigV1.0";

    private byte[] cacerts;

    private byte[] keystore;

    private Properties mainProps = new Properties();

    private char[] keystorpw;

    private char[] truststorepw;

    private GigiConfig() {}

    public byte[] getCacerts() {
        return cacerts;
    }

    public byte[] getKeystore() {
        return keystore;
    }

    public Properties getMainProps() {
        return mainProps;
    }

    public static GigiConfig parse(InputStream input) throws IOException {
        TarInputStream tis = new TarInputStream(input);
        TarEntry t;
        GigiConfig gc = new GigiConfig();
        while ((t = tis.getNextEntry()) != null) {
            if (t.getName().equals("gigi.properties")) {
                gc.mainProps.load(tis);
            } else if (t.getName().equals("cacerts.jks")) {
                gc.cacerts = readFully(tis);
            } else if (t.getName().equals("keystore.pkcs12")) {
                gc.keystore = readFully(tis);
            } else if (t.getName().equals("keystorepw")) {
                gc.keystorpw = transformSafe(readFully(tis));
            } else if (t.getName().equals("truststorepw")) {
                gc.truststorepw = transformSafe(readFully(tis));
            } else {
                System.out.println("Unknown config: " + t.getName());
            }
        }
        tis.close();
        return gc;
    }

    public static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        baos.close();
        return baos.toByteArray();
    }

    private static char[] transformSafe(byte[] readChunk) {
        char[] res = new char[readChunk.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (char) readChunk[i];
            readChunk[i] = 0;
        }
        return res;
    }

    public KeyStore getPrivateStore() throws GeneralSecurityException, IOException {
        KeyStore ks1 = KeyStore.getInstance("pkcs12");
        ks1.load(new ByteArrayInputStream(keystore), keystorpw);
        return ks1;
    }

    public KeyStore getTrustStore() throws GeneralSecurityException, IOException {
        KeyStore ks1 = KeyStore.getInstance("jks");
        ks1.load(new ByteArrayInputStream(cacerts), truststorepw);
        return ks1;
    }

    public String getPrivateStorePw() {
        return new String(keystorpw);
    }
}
