package org.cacert.gigi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.cacert.gigi.Certificate.CSRType;
import org.cacert.gigi.database.DatabaseConnection;

public class SimpleSigner {

    private static PreparedStatement warnMail;

    private static PreparedStatement updateMail;

    private static PreparedStatement readyCerts;

    private static PreparedStatement getSANSs;

    private static PreparedStatement revoke;

    private static PreparedStatement revokeCompleted;

    private static PreparedStatement finishJob;

    private static boolean running = true;

    private static Thread runner;

    public static void main(String[] args) throws IOException, SQLException, InterruptedException {
        Properties p = new Properties();
        p.load(new FileReader("config/gigi.properties"));
        DatabaseConnection.init(p);

        runSigner();
    }

    public synchronized static void stopSigner() throws InterruptedException {
        if (runner == null) {
            throw new IllegalStateException("already stopped");
        }
        running = false;
        runner.interrupt();
        runner.join();
        runner = null;
    }

    public synchronized static void runSigner() throws SQLException, IOException, InterruptedException {
        if (runner != null) {
            throw new IllegalStateException("already running");
        }
        running = true;
        readyCerts = DatabaseConnection.getInstance().prepare("SELECT certs.id AS id, certs.csr_name, certs.subject, jobs.id AS jobid, csr_type, md, keyUsage, extendedKeyUsage FROM jobs " + //
                "INNER JOIN certs ON certs.id=jobs.targetId " + //
                "INNER JOIN profiles ON profiles.id=certs.profile " + //
                "WHERE jobs.state='open' "//
                + "AND task='sign'");

        getSANSs = DatabaseConnection.getInstance().prepare("SELECT contents, type FROM subjectAlternativeNames " + //
                "WHERE certId=?");

        updateMail = DatabaseConnection.getInstance().prepare("UPDATE certs SET crt_name=?," + " created=NOW(), serial=? WHERE id=?");
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

    private static void work() {
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
                Thread.sleep(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e1) {
            }
        }
    }

    private static void revokeCertificates() throws SQLException, IOException, InterruptedException {
        ResultSet rs = revoke.executeQuery();
        boolean worked = false;
        while (rs.next()) {
            int id = rs.getInt(1);
            File crt = KeyStorage.locateCrt(id);
            String[] call = new String[] {
                    "openssl", "ca",//
                    "-cert",
                    "../unassured.crt",//
                    "-keyfile",
                    "../unassured.key",//
                    "-revoke",
                    "../../" + crt.getPath(),//
                    "-batch",//
                    "-config",
                    "../selfsign.config"

            };
            Process p1 = Runtime.getRuntime().exec(call, null, new File("keys/unassured.ca"));
            System.out.println("revoking: " + crt.getPath());
            if (p1.waitFor() == 0) {
                worked = true;
                revokeCompleted.setInt(1, id);
                revokeCompleted.execute();
                finishJob.setInt(1, rs.getInt(3));
                finishJob.execute();
            } else {
                System.out.println("Failed");
            }
        }
        if (worked) {
            gencrl();
        }
    }

    private static void gencrl() throws IOException, InterruptedException {
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

    private static void signCertificates() throws SQLException, IOException, InterruptedException {
        ResultSet rs = readyCerts.executeQuery();
        while (rs.next()) {
            String csrname = rs.getString("csr_name");
            System.out.println("sign: " + csrname);
            int id = rs.getInt("id");
            String csrType = rs.getString("csr_type");
            CSRType ct = CSRType.valueOf(csrType);
            File crt = KeyStorage.locateCrt(id);

            String keyUsage = rs.getString("keyUsage");
            String ekeyUsage = rs.getString("extendedKeyUsage");
            getSANSs.setInt(1, id);
            ResultSet san = getSANSs.executeQuery();

            File f = new File("keys", "SANFile" + System.currentTimeMillis() + (counter++) + ".cfg");
            PrintWriter cfg = new PrintWriter(f);
            boolean first = true;
            while (san.next()) {
                if ( !first) {
                    cfg.print(", ");
                } else {
                    cfg.print("subjectAltName=");
                }
                first = false;
                cfg.print(san.getString("type"));
                cfg.print(":");
                cfg.print(san.getString("contents"));
            }
            cfg.println();
            cfg.println("keyUsage=" + keyUsage);
            cfg.println("extendedKeyUsage=" + ekeyUsage);
            cfg.close();

            String[] call = new String[] {
                    "openssl", "ca",//
                    "-in",
                    "../../" + csrname,//
                    "-cert",
                    "../unassured.crt",//
                    "-keyfile",
                    "../unassured.key",//
                    "-out",
                    "../../" + crt.getPath(),//
                    "-days",
                    "356",//
                    "-batch",//
                    "-md",
                    rs.getString("md"),//
                    "-extfile",
                    "../" + f.getName(),//

                    "-subj",
                    rs.getString("subject"),//
                    "-config",
                    "../selfsign.config"//

            };
            if (ct == CSRType.SPKAC) {
                call[2] = "-spkac";
            }
            Process p1 = Runtime.getRuntime().exec(call, null, new File("keys/unassured.ca"));

            int waitFor = p1.waitFor();
            f.delete();
            if (waitFor == 0) {
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
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
                System.out.println("ERROR Afterwards: " + id);
                warnMail.setInt(1, rs.getInt("jobid"));
                warnMail.execute();
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
                String s;
                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
                System.out.println(Arrays.toString(call));
                System.out.println("ERROR: " + id);
                warnMail.setInt(1, rs.getInt("jobid"));
                warnMail.execute();
            }

        }
        rs.close();
    }
}
