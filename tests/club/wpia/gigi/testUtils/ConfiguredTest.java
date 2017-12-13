package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.DatabaseConnection.Link;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.SQLFileManager.ImportType;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.DatabaseManager;
import club.wpia.gigi.util.DomainAssessment;
import club.wpia.gigi.util.Notary;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.PasswordHash;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.TimeConditions;
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
            try {
                synchronized (ConfiguredTest.class) {
                    if (l == null) {
                        l = DatabaseConnection.newLink(false);
                    }
                }
            } catch (InterruptedException e) {
                throw new Error(e);
            }
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
                synchronized (ConfiguredTest.class) {
                    if (l == null) {
                        l = DatabaseConnection.newLink(false);
                    }
                }
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }

        return props;
    }

    @AfterClass
    public static void closeDBLink() {
        synchronized (ConfiguredTest.class) {
            if (l != null) {
                l.close();
                l = null;
            }
        }
    }

    private static Properties generateProps() throws Error {
        Properties mainProps = new Properties();
        mainProps.setProperty("name.secure", testProps.getProperty("name.secure"));
        mainProps.setProperty("name.www", testProps.getProperty("name.www"));
        mainProps.setProperty("name.static", testProps.getProperty("name.static"));
        mainProps.setProperty("name.api", testProps.getProperty("name.api"));
        mainProps.setProperty("name.suffix", testProps.getProperty("name.suffix"));

        mainProps.setProperty("appName", "SomeCA");
        mainProps.setProperty("appIdentifier", "someca");

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

    public static KeyPair generateBrokenKeypair() throws GeneralSecurityException {
        KeyPair keyPair = null;
        File f = new File("testBrokenKeypair");
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                keyPair = (KeyPair) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // -----BEGIN SHAMELESSLY ADAPTED BLOCK-----
            /**
             * Modified original RSA key generator to use three primes with one
             * prime set to fixed value to allow simple checking for such faulty
             * keys.
             *
             * @link sun.security.rsa.RSAKeyPairGenerator#generateKeyPair
             */

            KeyFactory factory = KeyFactory.getInstance("RSA");
            Random random = new SecureRandom();
            int keySize = 4096;
            long r_lv = 7331;

            // The generated numbers p q and r fall into the
            // following ranges:
            // - p: 2^(lp-1) < p < 2^lp
            // - q: 2^(lq-1) < q < 2^lq
            // - r: 2^12 < r < 2^13
            // Thus the generated number has at least lp+lq+11 bit and
            // can have at most lp+lq+13 bit.
            // Thus for random selection of p and q the algorithm will
            // at some point select a number of length n=n/2+lr+(n-n/2-lr)=>n
            // bit.
            int lp = (keySize + 1) >> 1;
            int lr = BigInteger.valueOf(r_lv).bitLength();
            int lq = keySize - lp - lr;

            BigInteger e = BigInteger.valueOf(7331);

            keyPair = null;
            while (keyPair == null) {
                // generate two random primes of size lp/lq
                BigInteger p, q, r, n;

                r = BigInteger.valueOf(r_lv);
                do {
                    p = BigInteger.probablePrime(lp, random);
                    q = BigInteger.probablePrime(lq, random);

                    // modulus n = p * q * r
                    n = p.multiply(q).multiply(r);

                    // even with correctly sized p, q and r, there is a chance
                    // that n will be one bit short. re-generate the
                    // primes if so.
                } while (n.bitLength() < keySize);

                // phi = (p - 1) * (q - 1) * (r - 1) must be relative prime to e
                // otherwise RSA just won't work ;-)
                BigInteger p1 = p.subtract(BigInteger.ONE);
                BigInteger q1 = q.subtract(BigInteger.ONE);
                BigInteger r1 = r.subtract(BigInteger.ONE);
                BigInteger phi = p1.multiply(q1).multiply(r1);

                // generate new p and q until they work. typically
                if (e.gcd(phi).equals(BigInteger.ONE) == false) {
                    continue;
                }

                // private exponent d is the inverse of e mod phi
                BigInteger d = e.modInverse(phi);

                RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(n, e);
                RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(n, d);
                PublicKey publicKey = factory.generatePublic(publicSpec);
                PrivateKey privateKey = factory.generatePrivate(privateSpec);
                keyPair = new KeyPair(publicKey, privateKey);
            }
            // -----END SHAMELESSLY ADAPTED BLOCK-----

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(keyPair);
                oos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
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

    public static void makeAgent(int uid) {
        try (GigiPreparedStatement ps1 = new GigiPreparedStatement("INSERT INTO cats_passed SET user_id=?, variant_id=?, language='en_EN', version='1'")) {
            ps1.setInt(1, uid);
            ps1.setInt(2, CATSType.AGENT_CHALLENGE.getId());
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
            TestMail testMail = getMailReceiver().receive("admin@" + d.getSuffix());
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
