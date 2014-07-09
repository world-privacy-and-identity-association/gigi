package org.cacert.gigi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.cacert.gigi.database.DatabaseConnection;

public class SimpleSigner {
	private static PreparedStatement warnMail;
	private static PreparedStatement updateMail;
	private static PreparedStatement readyMail;
	private static PreparedStatement revoke;
	private static PreparedStatement revokeCompleted;

	public static void main(String[] args) throws IOException, SQLException, InterruptedException {
		Properties p = new Properties();
		p.load(new FileReader("config/gigi.properties"));
		DatabaseConnection.init(p);

		readyMail = DatabaseConnection.getInstance().prepare(
			"SELECT id, csr_name, subject FROM emailcerts" + " WHERE csr_name is not null"//
				+ " AND created=0"//
				+ " AND crt_name=''"//
				+ " AND warning<3");

		updateMail = DatabaseConnection.getInstance().prepare(
			"UPDATE emailcerts SET crt_name=?," + " created=NOW(), serial=? WHERE id=?");
		warnMail = DatabaseConnection.getInstance().prepare("UPDATE emailcerts SET warning=warning+1 WHERE id=?");

		revoke = DatabaseConnection.getInstance().prepare(
			"SELECT id, csr_name FROM emailcerts" + " WHERE csr_name is not null"//
				+ " AND created != 0"//
				+ " AND revoked = '1970-01-01'");
		revokeCompleted = DatabaseConnection.getInstance().prepare("UPDATE emailcerts SET revoked=NOW() WHERE id=?");
		gencrl();
		while (true) {
			System.out.println("ping");
			signCertificates();
			revokeCertificates();
			Thread.sleep(5000);
		}
	}

	private static void revokeCertificates() throws SQLException, IOException, InterruptedException {
		ResultSet rs = revoke.executeQuery();
		boolean worked = false;
		while (rs.next()) {
			int id = rs.getInt(1);
			File crt = KeyStorage.locateCrt(id);
			String[] call = new String[] { "openssl", "ca",//
					"-cert", "testca.crt",//
					"-keyfile", "testca.key",//
					"-revoke", "../" + crt.getPath(),//
					"-batch",//
					"-config", "selfsign.config"

			};
			Process p1 = Runtime.getRuntime().exec(call, null, new File("keys"));
			System.out.println("revoking: " + crt.getPath());
			if (p1.waitFor() == 0) {
				worked = true;
				revokeCompleted.setInt(1, id);
				revokeCompleted.execute();
			} else {
				System.out.println("Failed");
			}
		}
		if (worked) {
			gencrl();
		}
	}

	private static void gencrl() throws IOException, InterruptedException {
		String[] call = new String[] { "openssl", "ca",//
				"-cert", "testca.crt",//
				"-keyfile", "testca.key",//
				"-gencrl",//
				"-crlhours",//
				"12",//
				"-out", "testca.crl",//
				"-config", "selfsign.config"

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
			File crt = KeyStorage.locateCrt(id);
			String[] call = new String[] { "openssl", "ca",//
					"-cert", "testca.crt",//
					"-keyfile", "testca.key",//
					"-in", "../" + csrname,//
					"-out", "../" + crt.getPath(),//
					"-days", "356",//
					"-batch",//
					"-subj", rs.getString(3),//
					"-config", "selfsign.config"

			};
			Process p1 = Runtime.getRuntime().exec(call, null, new File("keys"));

			int waitFor = p1.waitFor();
			if (waitFor == 0) {
				try (InputStream is = new FileInputStream(crt)) {
					CertificateFactory cf = CertificateFactory.getInstance("X.509");
					X509Certificate crtp = (X509Certificate) cf.generateCertificate(is);
					BigInteger serial = crtp.getSerialNumber();
					updateMail.setString(1, crt.getPath());
					updateMail.setString(2, serial.toString());
					updateMail.setInt(3, id);
					updateMail.execute();
					System.out.println("sign: " + id);
					continue;
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
				System.out.println("ERROR: " + id);
				warnMail.setInt(1, id);
				warnMail.execute();
			} else {
				System.out.println("ERROR: " + id);
				warnMail.setInt(1, id);
				warnMail.execute();
			}

		}
		rs.close();
	}
}
