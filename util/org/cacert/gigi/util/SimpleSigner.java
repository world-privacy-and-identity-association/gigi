package org.cacert.gigi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static PreparedStatement readyMail;

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
        readyMail = DatabaseConnection.getInstance().prepare("SELECT emailcerts.id,emailcerts.csr_name,emailcerts.subject, jobs.id,csr_type FROM jobs INNER JOIN emailcerts ON emailcerts.id=jobs.targetId" + " WHERE jobs.state='open'"//
                + " AND task='sign'");

        updateMail = DatabaseConnection.getInstance().prepare("UPDATE emailcerts SET crt_name=?," + " created=NOW(), serial=? WHERE id=?");
        warnMail = DatabaseConnection.getInstance().prepare("UPDATE jobs SET warning=warning+1, state=IF(warning<3, 'open','error') WHERE id=?");

        revoke = DatabaseConnection.getInstance().prepare("SELECT emailcerts.id, emailcerts.csr_name,jobs.id FROM jobs INNER JOIN emailcerts ON jobs.targetId=emailcerts.id" + " WHERE jobs.state='open' AND task='revoke'");
        revokeCompleted = DatabaseConnection.getInstance().prepare("UPDATE emailcerts SET revoked=NOW() WHERE id=?");

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
                    "testca.crt",//
                    "-keyfile",
                    "testca.key",//
                    "-revoke",
                    "../" + crt.getPath(),//
                    "-batch",//
                    "-config",
                    "selfsign.config"

            };
            Process p1 = Runtime.getRuntime().exec(call, null, new File("keys"));
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
                "testca.crt",//
                "-keyfile",
                "testca.key",//
                "-gencrl",//
                "-crlhours",//
                "12",//
                "-out",
                "testca.crl",//
                "-config",
                "selfsign.config"

        };
        Process p1 = Runtime.getRuntime().exec(call, null, new File("keys"));
        if (p1.waitFor() != 0) {
            System.out.println("Error while generating crl.");
        }
    }

    private static void signCertificates() throws SQLException, IOException, InterruptedException {
        ResultSet rs = readyMail.executeQuery();
        while (rs.next()) {
            String csrname = rs.getString(2);
            System.out.println("sign: " + csrname);
            int id = rs.getInt(1);
            String csrType = rs.getString(5);
            CSRType ct = CSRType.valueOf(csrType);
            File crt = KeyStorage.locateCrt(id);
            String[] call = new String[] {
                    "openssl", "ca",//
                    "-in",
                    "../" + csrname,//
                    "-cert",
                    "testca.crt",//
                    "-keyfile",
                    "testca.key",//
                    "-out",
                    "../" + crt.getPath(),//
                    "-days",
                    "356",//
                    "-batch",//
                    "-subj",
                    rs.getString(3),//
                    "-config",
                    "selfsign.config"//

            };
            if (ct == CSRType.SPKAC) {
                call[2] = "-spkac";
            }
            Process p1 = Runtime.getRuntime().exec(call, null, new File("keys"));

            int waitFor = p1.waitFor();
            if (waitFor == 0) {
                try (InputStream is = new FileInputStream(crt)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate crtp = (X509Certificate) cf.generateCertificate(is);
                    BigInteger serial = crtp.getSerialNumber();
                    updateMail.setString(1, crt.getPath());
                    updateMail.setString(2, serial.toString(16));
                    updateMail.setInt(3, id);
                    updateMail.execute();

                    finishJob.setInt(1, rs.getInt(4));
                    finishJob.execute();
                    System.out.println("signed: " + id);
                    continue;
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
                System.out.println("ERROR Afterwards: " + id);
                warnMail.setInt(1, rs.getInt(4));
                warnMail.execute();
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
                String s;
                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
                System.out.println(Arrays.toString(call));
                System.out.println("ERROR: " + id);
                warnMail.setInt(1, rs.getInt(4));
                warnMail.execute();
            }

        }
        rs.close();
    }
}
