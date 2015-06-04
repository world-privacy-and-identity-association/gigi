package org.cacert.gigi.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import javax.security.auth.x500.X500Principal;

import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CSRType;
import org.cacert.gigi.dbObjects.Certificate.SANType;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.output.DateSelector;

import sun.security.pkcs10.PKCS10;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AVA;
import sun.security.x509.AlgorithmId;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.RDN;
import sun.security.x509.X500Name;

public class SimpleSigner {

    private static GigiPreparedStatement warnMail;

    private static GigiPreparedStatement updateMail;

    private static GigiPreparedStatement readyCerts;

    private static GigiPreparedStatement getSANSs;

    private static GigiPreparedStatement revoke;

    private static GigiPreparedStatement revokeCompleted;

    private static GigiPreparedStatement finishJob;

    private static volatile boolean running = true;

    private static Thread runner;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss'Z'");

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        if (false) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                X500Principal xp = new X500Principal(genX500Name(Certificate.buildDN("CN", "uiae")).getEncoded());
                byte[] i = generateCert(kp.getPublic(), kp.getPrivate(), Certificate.buildDN("CN", "uiae"), xp, Arrays.<SubjectAlternateName>asList(), new Date(), new Date(System.currentTimeMillis() + 1000 * 60 * 60), Digest.SHA512, "clientAuth");
                System.out.println(Base64.getMimeEncoder().encodeToString(i));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            return;
        }
        Properties p = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream("config/gigi.properties"), "UTF-8")) {
            p.load(reader);
        }
        DatabaseConnection.init(p);

        runSigner();
    }

    public static void stopSigner() throws InterruptedException {
        Thread capturedRunner;
        synchronized (SimpleSigner.class) {
            if (runner == null) {
                throw new IllegalStateException("already stopped");
            }
            capturedRunner = runner;
            running = false;
            SimpleSigner.class.notifyAll();
        }
        capturedRunner.join();
    }

    public synchronized static void runSigner() throws SQLException, IOException, InterruptedException {
        if (runner != null) {
            throw new IllegalStateException("already running");
        }
        running = true;
        readyCerts = DatabaseConnection.getInstance().prepare("SELECT certs.id AS id, certs.csr_name, jobs.id AS jobid, csr_type, md, executeFrom, executeTo, profile FROM jobs " + //
                "INNER JOIN certs ON certs.id=jobs.targetId " + //
                "INNER JOIN profiles ON profiles.id=certs.profile " + //
                "WHERE jobs.state='open' "//
                + "AND task='sign'");

        getSANSs = DatabaseConnection.getInstance().prepare("SELECT contents, type FROM subjectAlternativeNames " + //
                "WHERE certId=?");

        updateMail = DatabaseConnection.getInstance().prepare("UPDATE certs SET crt_name=?," + " created=NOW(), serial=?, caid=1 WHERE id=?");
        warnMail = DatabaseConnection.getInstance().prepare("UPDATE jobs SET warning=warning+1, state=IF(warning<3, 'open','error') WHERE id=?");

        revoke = DatabaseConnection.getInstance().prepare("SELECT certs.id, certs.csr_name,jobs.id FROM jobs INNER JOIN certs ON jobs.targetId=certs.id" + " WHERE jobs.state='open' AND task='revoke'");
        revokeCompleted = DatabaseConnection.getInstance().prepare("UPDATE certs SET revoked=NOW() WHERE id=?");

        finishJob = DatabaseConnection.getInstance().prepare("UPDATE jobs SET state='done' WHERE id=?");

        runner = new Thread() {

            @Override
            public void run() {
                work();
            }

        };
        runner.start();
    }

    private synchronized static void work() {
        try {
            gencrl();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }

        while (running) {
            try {
                signCertificates();
                revokeCertificates();

                SimpleSigner.class.wait(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e1) {
            }
        }
        runner = null;
    }

    private static void revokeCertificates() throws SQLException, IOException, InterruptedException {
        GigiResultSet rs = revoke.executeQuery();
        boolean worked = false;
        while (rs.next()) {
            int id = rs.getInt(1);
            File crt = KeyStorage.locateCrt(id);
            worked = true;
            revokeCompleted.setInt(1, id);
            revokeCompleted.execute();
            finishJob.setInt(1, rs.getInt(3));
            finishJob.execute();
        }
        if (worked) {
            gencrl();
        }
    }

    private static void gencrl() throws IOException, InterruptedException {
        if (true) {
            return;
        }
        String[] call = new String[] {
                "openssl", "ca",//
                "-cert",
                "../unassured.crt",//
                "-keyfile",
                "../unassured.key",//
                "-gencrl",//
                "-crlhours",//
                "12",//
                "-out",
                "../unassured.crl",//
                "-config",
                "../selfsign.config"

        };
        Process p1 = Runtime.getRuntime().exec(call, null, new File("keys/unassured.ca"));
        if (p1.waitFor() != 0) {
            System.out.println("Error while generating crl.");
        }
    }

    private static int counter = 0;

    private static void signCertificates() throws SQLException {
        GigiResultSet rs = readyCerts.executeQuery();

        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        while (rs.next()) {
            String csrname = rs.getString("csr_name");
            int id = rs.getInt("id");
            System.out.println("sign: " + csrname);
            try {
                String csrType = rs.getString("csr_type");
                CSRType ct = CSRType.valueOf(csrType);
                File crt = KeyStorage.locateCrt(id);

                Timestamp from = rs.getTimestamp("executeFrom");
                String length = rs.getString("executeTo");
                Date fromDate;
                Date toDate;
                if (from == null) {
                    fromDate = new Date(System.currentTimeMillis());
                } else {
                    fromDate = new Date(from.getTime());
                }
                if (length.endsWith("m") || length.endsWith("y")) {
                    String num = length.substring(0, length.length() - 1);
                    int inter = Integer.parseInt(num);
                    c.setTime(fromDate);
                    if (length.endsWith("m")) {
                        c.add(Calendar.MONTH, inter);
                    } else {
                        c.add(Calendar.YEAR, inter);
                    }
                    toDate = c.getTime();
                } else {
                    toDate = DateSelector.getDateFormat().parse(length);
                }

                getSANSs.setInt(1, id);
                GigiResultSet san = getSANSs.executeQuery();

                boolean first = true;
                LinkedList<SubjectAlternateName> altnames = new LinkedList<>();
                while (san.next()) {
                    first = false;
                    altnames.add(new SubjectAlternateName(SANType.valueOf(san.getString("type").toUpperCase()), san.getString("contents")));
                }
                // TODO look them up!
                // cfg.println("keyUsage=critical," +
                // "digitalSignature, keyEncipherment, keyAgreement");
                // cfg.println("extendedKeyUsage=critical," + "clientAuth");
                // cfg.close();

                int profile = rs.getInt("profile");
                CertificateProfile cp = CertificateProfile.getById(profile);
                String s = cp.getId() + "";
                while (s.length() < 4) {
                    s = "0" + s;
                }
                s += "-" + cp.getKeyName() + ".cfg";
                Properties caP = new Properties();
                caP.load(new FileInputStream("signer/profiles/" + s));
                String ca = caP.getProperty("ca") + "_2015_1";

                HashMap<String, String> subj = new HashMap<>();
                GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT name, value FROM certAvas WHERE certId=?");
                ps.setInt(1, rs.getInt("id"));
                GigiResultSet rs2 = ps.executeQuery();
                while (rs2.next()) {
                    String name = rs2.getString("name");
                    if (name.equals("EMAIL")) {
                        name = "emailAddress";
                    }
                    subj.put(name, rs2.getString("value"));
                }
                if (subj.size() == 0) {
                    subj.put("CN", "<empty>");
                    System.out.println("WARNING: DN was empty");
                }
                System.out.println(subj);

                PublicKey pk;
                byte[] data = IOUtils.readURL(new FileInputStream(csrname));
                if (ct == CSRType.SPKAC) {
                    String dt = new String(data);
                    if (dt.startsWith("SPKAC=")) {
                        dt = dt.substring(6);
                        data = dt.getBytes();
                        System.out.println(dt);
                    }
                    SPKAC sp = new SPKAC(Base64.getDecoder().decode(data));
                    pk = sp.getPubkey();
                } else {
                    PKCS10 p10 = new PKCS10(PEM.decode("(NEW )?CERTIFICATE REQUEST", new String(data)));
                    pk = p10.getSubjectPublicKeyInfo();
                }
                PrivateKey i = loadOpensslKey(new File("signer/ca/" + ca + "/ca.key"));

                String[] call;
                X509Certificate root = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new FileInputStream("signer/ca/" + ca + "/ca.crt"));
                byte[] cert = generateCert(pk, i, subj, root.getSubjectX500Principal(), altnames, fromDate, toDate, Digest.valueOf(rs.getString("md").toUpperCase()), caP.getProperty("eku"));
                PrintWriter out = new PrintWriter(crt);
                out.println("-----BEGIN CERTIFICATE-----");
                out.println(Base64.getMimeEncoder().encodeToString(cert));
                out.println("-----END CERTIFICATE-----");
                out.close();
                synchronized (sdf) {
                    /*
                     * call = new String[] { "openssl", "ca",// "-in", "../../"
                     * + csrname,// "-cert", "../" + ca + ".crt",// "-keyfile",
                     * "../" + ca + ".key",// "-out", "../../" +
                     * crt.getPath(),// "-utf8", "-startdate",
                     * sdf.format(fromDate),// "-enddate", sdf.format(toDate),//
                     * "-batch",// "-md", rs.getString("md"),// "-extfile",
                     * "../" + f.getName(),// "-subj",
                     * Certificate.stringifyDN(subj),// "-config",
                     * "../selfsign.config"// };
                     */
                }

                try (InputStream is = new FileInputStream(crt)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate crtp = (X509Certificate) cf.generateCertificate(is);
                    BigInteger serial = crtp.getSerialNumber();
                    updateMail.setString(1, crt.getPath());
                    updateMail.setString(2, serial.toString(16));
                    updateMail.setInt(3, id);
                    updateMail.execute();

                    finishJob.setInt(1, rs.getInt("jobid"));
                    finishJob.execute();
                    System.out.println("signed: " + id);
                    continue;
                }

            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            System.out.println("Error with: " + id);
            warnMail.setInt(1, rs.getInt("jobid"));
            warnMail.execute();

        }
        rs.close();
    }

    private static PrivateKey loadOpensslKey(File f) throws FileNotFoundException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] p8b = PEM.decode("RSA PRIVATE KEY", new String(IOUtils.readURL(new FileInputStream(f))));
        DerOutputStream dos = new DerOutputStream();
        dos.putInteger(0);
        new AlgorithmId(new ObjectIdentifier(new int[] {
                1, 2, 840, 113549, 1, 1, 1
        })).encode(dos);
        dos.putOctetString(p8b);
        byte[] ctx = dos.toByteArray();
        dos.reset();
        dos.write(DerValue.tag_Sequence, ctx);
        PKCS8EncodedKeySpec p8 = new PKCS8EncodedKeySpec(dos.toByteArray());
        PrivateKey i = KeyFactory.getInstance("RSA").generatePrivate(p8);
        return i;
    }

    private static synchronized byte[] generateCert(PublicKey pk, PrivateKey prk, Map<String, String> subj, X500Principal issuer, List<SubjectAlternateName> altnames, Date fromDate, Date toDate, Digest digest, String eku) throws IOException, GeneralSecurityException {
        File f = Paths.get("signer", "serial").toFile();
        if ( !f.exists()) {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write("1".getBytes());
            fos.close();
        }
        try (FileInputStream fr = new FileInputStream(f)) {
            byte[] serial = IOUtils.readURL(fr);
            BigInteger ser = new BigInteger(new String(serial).trim());
            ser = ser.add(BigInteger.ONE);

            PrintWriter pw = new PrintWriter(f);
            pw.println(ser);
            pw.close();
            if (digest != Digest.SHA256 && digest != Digest.SHA512) {
                System.err.println("assuming sha256 either way ;-): " + digest);
                digest = Digest.SHA256;
            }
            ObjectIdentifier sha512withrsa = new ObjectIdentifier(new int[] {
                    1, 2, 840, 113549, 1, 1, digest == Digest.SHA256 ? 11 : 13
            });
            AlgorithmId aid = new AlgorithmId(sha512withrsa);
            Signature s = Signature.getInstance(digest == Digest.SHA256 ? "SHA256withRSA" : "SHA512withRSA");

            DerOutputStream cert = new DerOutputStream();
            DerOutputStream content = new DerOutputStream();
            {
                DerOutputStream version = new DerOutputStream();
                version.putInteger(2); // v3
                content.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0), version);
            }
            content.putInteger(ser); // Serial
            aid.encode(content);

            {
                content.write(issuer.getEncoded());
            }
            {
                DerOutputStream notAround = new DerOutputStream();
                notAround.putUTCTime(fromDate);
                notAround.putUTCTime(toDate);
                content.write(DerValue.tag_Sequence, notAround);
            }
            {

                X500Name xn = genX500Name(subj);
                content.write(xn.getEncoded());
            }
            {
                content.write(pk.getEncoded());
            }
            {
                DerOutputStream extensions = new DerOutputStream();
                {
                    addExtension(extensions, new ObjectIdentifier(new int[] {
                            2, 5, 29, 17
                    }), generateSAN(altnames));
                    addExtension(extensions, new ObjectIdentifier(new int[] {
                            2, 5, 29, 15
                    }), generateKU());
                    addExtension(extensions, new ObjectIdentifier(new int[] {
                            2, 5, 29, 37
                    }), generateEKU(eku));
                }
                DerOutputStream extensionsSeq = new DerOutputStream();
                extensionsSeq.write(DerValue.tag_Sequence, extensions);
                content.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 3), extensionsSeq);
            }

            DerOutputStream contentSeq = new DerOutputStream();

            contentSeq.write(DerValue.tag_Sequence, content.toByteArray());

            s.initSign(prk);
            s.update(contentSeq.toByteArray());

            aid.encode(contentSeq);
            contentSeq.putBitString(s.sign());
            cert.write(DerValue.tag_Sequence, contentSeq);

            X509Certificate c = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(cert.toByteArray()));
            // c.verify(pk); only for self-signeds

            return cert.toByteArray();
        }

    }

    private static byte[] generateKU() throws IOException {
        DerOutputStream dos = new DerOutputStream();
        dos.putBitString(new byte[] {
            (byte) 0b10101000
        });
        return dos.toByteArray();
    }

    private static byte[] generateEKU(String eku) throws IOException {

        DerOutputStream dos = new DerOutputStream();
        for (String name : eku.split(",")) {
            ObjectIdentifier oid;
            switch (name) {
            case "serverAuth":
                oid = new ObjectIdentifier("1.3.6.1.5.5.7.3.1");
                break;
            case "clientAuth":
                oid = new ObjectIdentifier("1.3.6.1.5.5.7.3.2");
                break;
            case "codeSigning":
                oid = new ObjectIdentifier("1.3.6.1.5.5.7.3.3");
                break;
            case "emailProtection":
                oid = new ObjectIdentifier("1.3.6.1.5.5.7.3.4");
                break;

            default:
                throw new Error(name);
            }
            dos.putOID(oid);
        }
        byte[] data = dos.toByteArray();
        dos.reset();
        dos.write(DerValue.tag_Sequence, data);
        return dos.toByteArray();
    }

    private static X500Name genX500Name(Map<String, String> subj) throws IOException {
        LinkedList<RDN> rdns = new LinkedList<>();
        for (Entry<String, String> i : subj.entrySet()) {
            RDN rdn = genRDN(i);
            rdns.add(rdn);
        }
        return new X500Name(rdns.toArray(new RDN[rdns.size()]));
    }

    private static RDN genRDN(Entry<String, String> i) throws IOException {
        DerOutputStream dos = new DerOutputStream();
        dos.putUTF8String(i.getValue());
        int[] oid;
        String key = i.getKey();
        switch (key) {
        case "CN":
            oid = new int[] {
                    2, 5, 4, 3
            };
            break;
        case "EMAIL":
        case "emailAddress":
            oid = new int[] {
                    1, 2, 840, 113549, 1, 9, 1
            };
            break;
        case "O":
            oid = new int[] {
                    2, 5, 4, 10
            };
            break;
        case "OU":
            oid = new int[] {
                    2, 5, 4, 11
            };
            break;
        default:
            throw new Error("unknown RDN-type: " + key);
        }
        RDN rdn = new RDN(new AVA(new ObjectIdentifier(oid), new DerValue(dos.toByteArray())));
        return rdn;
    }

    private static void addExtension(DerOutputStream extensions, ObjectIdentifier oid, byte[] extContent) throws IOException {
        DerOutputStream SANs = new DerOutputStream();
        SANs.putOID(oid);
        SANs.putOctetString(extContent);

        extensions.write(DerValue.tag_Sequence, SANs);
    }

    private static byte[] generateSAN(List<SubjectAlternateName> altnames) throws IOException {
        DerOutputStream SANContent = new DerOutputStream();
        for (SubjectAlternateName san : altnames) {
            byte type = 0;
            if (san.getType() == SANType.DNS) {
                type = (byte) GeneralNameInterface.NAME_DNS;
            } else if (san.getType() == SANType.EMAIL) {
                type = (byte) GeneralNameInterface.NAME_RFC822;
            } else {
                throw new Error("" + san.getType());
            }
            SANContent.write(DerValue.createTag(DerValue.TAG_CONTEXT, false, type), san.getName().getBytes());
        }
        DerOutputStream SANSeqContent = new DerOutputStream();
        SANSeqContent.write(DerValue.tag_Sequence, SANContent);
        byte[] byteArray = SANSeqContent.toByteArray();
        return byteArray;
    }
}
