package org.cacert.gigi.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.cacert.gigi.database.DatabaseConnection;

public class SimpleSigner {
	private static PreparedStatement warnMail;
	private static PreparedStatement updateMail;
	private static PreparedStatement readyMail;

	public static void main(String[] args) throws IOException, SQLException,
			InterruptedException {
		Properties p = new Properties();
		p.load(new FileReader("config/gigi.properties"));
		DatabaseConnection.init(p);

		readyMail = DatabaseConnection.getInstance().prepare(
				"SELECT id, csr_name FROM emailcerts" + " WHERE csr_name!=null"//
						+ " AND created=0"//
						+ " AND crt_name=''"//
						+ " AND warning<3");

		updateMail = DatabaseConnection.getInstance().prepare(
				"UPDATE emailcerts SET crt_name=?,"
						+ " created=NOW() WHERE id=?");
		warnMail = DatabaseConnection.getInstance().prepare(
				"UPDATE emailcerts SET warning=warning+1 WHERE id=?");
		while (true) {
			System.out.println("ping");
			executeOutstanders();
			Thread.sleep(5000);
		}
	}

	private static void executeOutstanders() throws SQLException, IOException,
			InterruptedException {
		ResultSet rs = readyMail.executeQuery();
		while (rs.next()) {
			String csrname = rs.getString(2);
			System.out.println("sign: " + csrname);
			int id = rs.getInt(1);
			File crt = KeyStorage.locateCrt(id);
			String[] call = new String[]{"openssl", "ca",//
					"-cert", "testca.crt",//
					"-keyfile", "testca.key",//
					"-in", "../" + csrname,//
					"-out", "../" + crt.getPath(),//
					"-days", "356",//
					"-batch",//
					"-config", "selfsign.config"

			};
			Process p1 = Runtime.getRuntime()
					.exec(call, null, new File("keys"));

			int waitFor = p1.waitFor();
			if (waitFor == 0) {
				updateMail.setString(1, crt.getPath());
				updateMail.setInt(2, id);
				updateMail.execute();
				System.out.println("sign: " + id);
			} else {
				System.out.println("ERROR: " + id);
				warnMail.setInt(1, id);
				warnMail.execute();
			}

		}
		rs.close();
	}
}
