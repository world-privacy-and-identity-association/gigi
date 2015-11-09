package org.cacert.gigi.testUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.PEM;
import org.junit.BeforeClass;

import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attributes;
import sun.security.x509.X500Name;

/**
 * Base class for a Testsuite that makes use of the config variables that define
 * the environment.
 */
public abstract class ConfiguredTest {

    static Properties testProps = new Properties();

    public static Properties getTestProps() {
        return testProps;
    }

    private static boolean envInited = false;

    @BeforeClass
    public static void initEnvironment() throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        if (envInited) {
            return;
        }
        envInited = true;
        try (FileInputStream inStream = new FileInputStream("config/test.properties")) {
            testProps.load(inStream);
        }
        if ( !DatabaseConnection.isInited()) {
            DatabaseConnection.init(testProps);
        }
    }

    public static KeyPair generateKeypair() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096);
        KeyPair keyPair = null;
        File f = new File("testKeypair");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                keyPair = (KeyPair) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            keyPair = kpg.generateKeyPair();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(keyPair);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return keyPair;
    }

    public static String generatePEMCSR(KeyPair kp, String dn) throws GeneralSecurityException, IOException {
        return generatePEMCSR(kp, dn, new PKCS10Attributes());
    }

    public static String generatePEMCSR(KeyPair kp, String dn, PKCS10Attributes atts) throws GeneralSecurityException, IOException {
        return generatePEMCSR(kp, dn, atts, "SHA256WithRSA");
    }

    public static String generatePEMCSR(KeyPair kp, String dn, PKCS10Attributes atts, String signature) throws GeneralSecurityException, IOException {
        PKCS10 p10 = new PKCS10(kp.getPublic(), atts);
        Signature s = Signature.getInstance(signature);
        s.initSign(kp.getPrivate());
        p10.encodeAndSign(new X500Name(dn), s);
        return PEM.encode("CERTIFICATE REQUEST", p10.getEncoded());
    }

    static int count = 0;

    public static String createUniqueName() {
        return "test" + System.currentTimeMillis() + "a" + (count++) + "u";
    }

    public static int countRegex(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        int i = 0;
        while (m.find()) {
            i++;
        }
        return i;
    }
}
