package org.cacert.gigi.testUtils;

import static org.junit.Assert.*;

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
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.DatabaseConnection.Link;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.SQLFileManager.ImportType;
import org.cacert.gigi.dbObjects.CATS.CATSType;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingType;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.cacert.gigi.util.DatabaseManager;
import org.cacert.gigi.util.DomainAssessment;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PEM;
import org.cacert.gigi.util.PasswordHash;
import org.cacert.gigi.util.ServerConstants;
import org.cacert.gigi.util.TimeConditions;
import org.junit.AfterClass;
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

    /**
     * Some password that fulfills the password criteria.
     */
    public static final String TEST_PASSWORD = "xvXV12°§";

    public static final String DIFFICULT_CHARS = "ÜÖÄß𐀀";

    @BeforeClass
    public static void initEnvironmentHook() throws IOException {
        initEnvironment();
    }

    private static Link l;

    public static Properties initEnvironment() throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        if (envInited) {
            return generateProps();
        }
        envInited = true;
        try (FileInputStream inStream = new FileInputStream("config/test.properties")) {
            testProps.load(inStream);
        }
        Properties props = generateProps();
        ServerConstants.init(props);
        TimeConditions.init(props);
        DomainAssessment.init(props);
        PasswordHash.init(props);

        if ( !DatabaseConnection.isInited()) {
            DatabaseConnection.init(testProps);
            try {
                l = DatabaseConnection.newLink(false);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        return props;
    }

    @AfterClass
    public static void closeDBLink() {
        if (l != null) {
            l.close();
            l = null;
        }
    }

    private static Properties generateProps() throws Error {
        Properties mainProps = new Properties();
        mainProps.setProperty("name.secure", testProps.getProperty("name.secure"));
        mainProps.setProperty("name.www", testProps.getProperty("name.www"));
        mainProps.setProperty("name.static", testProps.getProperty("name.static"));
        mainProps.setProperty("name.api", testProps.getProperty("name.api"));

        mainProps.setProperty("https.port", testProps.getProperty("serverPort.https"));
        mainProps.setProperty("http.port", testProps.getProperty("serverPort.http"));

        File out = new File("financial.dat");
        if ( !out.exists()) {
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write("google.com\ntwitter.com\n".getBytes("UTF-8"));
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        mainProps.setProperty("highFinancialValue", out.getAbsolutePath());
        mainProps.setProperty("scrypt.params", "1;1;1");
        return mainProps;
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
        return generatePEMCSR(kp, dn, atts, "SHA512WithRSA");
    }

    public static String generatePEMCSR(KeyPair kp, String dn, PKCS10Attributes atts, String signature) throws GeneralSecurityException, IOException {
        PKCS10 p10 = new PKCS10(kp.getPublic(), atts);
        Signature s = Signature.getInstance(signature);
        s.initSign(kp.getPrivate());
        p10.encodeAndSign(new X500Name(dn), s);
        return PEM.encode("CERTIFICATE REQUEST", p10.getEncoded());
    }

    static int count = 0;

    public static String createRandomIDString() {
        final char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        final int idStringLength = 16;

        Random sr;
        sr = new Random();

        StringBuilder sb = new StringBuilder(idStringLength);
        for (int i = 0; i < idStringLength; i++) {
            sb.append(chars[sr.nextInt(chars.length)]);
        }

        return sb.toString();
    }

    public static synchronized String createUniqueName() {
        return "test" + createRandomIDString() + "a" + (count++) + "u";
    }

    public static CertificateProfile getClientProfile() {
        return CertificateProfile.getByName("client");
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

    public static void makeAssurer(int uid) {
        try (GigiPreparedStatement ps1 = new GigiPreparedStatement("INSERT INTO cats_passed SET user_id=?, variant_id=?, language='en_EN', version='1'")) {
            ps1.setInt(1, uid);
            ps1.setInt(2, CATSType.ASSURER_CHALLENGE.getId());
            ps1.execute();
        }

        try (GigiPreparedStatement ps2 = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, points='100'")) {
            ps2.setInt(1, uid);
            ps2.setInt(2, User.getById(uid).getPreferredName().getId());
            ps2.execute();
        }
    }

    public MailReceiver getMailReceiver() {
        throw new Error("Feature requires Business or ManagedTest.");
    }

    public void verify(Domain d) {
        try {
            d.addPing(DomainPingType.EMAIL, "admin");
            TestMail testMail = getMailReceiver().receive();
            testMail.verify();
            assertTrue(d.isVerified());
        } catch (GigiApiException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static void purgeOnlyDB() throws SQLException, IOException {
        System.out.println("... resetting Database");
        long ms = System.currentTimeMillis();
        try {
            DatabaseManager.run(new String[] {
                    testProps.getProperty("sql.driver"), testProps.getProperty("sql.url"), testProps.getProperty("sql.user"), testProps.getProperty("sql.password")
            }, ImportType.TRUNCATE);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Database reset complete in " + (System.currentTimeMillis() - ms) + " ms.");
    }

    public static String validVerificationDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION + 1);
        return sdf.format(new Date(c.getTimeInMillis()));
    }
}
