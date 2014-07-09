package org.cacert.gigi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.KeyStorage;

public class Certificate {
	private int id;
	private int serial;
	private String dn;
	private String md;
	private String csrName;
	private String crtName;
	private String csr = null;
	public Certificate(String dn, String md, String csr) {
		this.dn = dn;
		this.md = md;
		this.csr = csr;
	}

	public enum CertificateStatus {
		DRAFT(false), BEING_ISSUED(true), ISSUED(false), BEING_REVOKED(true), REVOKED(
				false);

		private boolean unstable;

		private CertificateStatus(boolean unstable) {
			this.unstable = unstable;
		}
		public boolean isUnstable() {
			return unstable;
		}

	}
	public CertificateStatus getStatus() throws SQLException {
		if (id == 0) {
			return CertificateStatus.DRAFT;
		}
		PreparedStatement searcher = DatabaseConnection.getInstance().prepare(
				"SELECT crt_name, created, revoked FROM emailcerts WHERE id=?");
		searcher.setInt(1, id);
		ResultSet rs = searcher.executeQuery();
		if (!rs.next()) {
			throw new IllegalStateException("Certificate not in Database");
		}
		if (rs.getString(2) == null) {
			return CertificateStatus.BEING_ISSUED;
		}
		crtName = rs.getString(1);
		System.out.println(crtName);
		if (rs.getTime(2) != null && rs.getTime(3) == null) {
			return CertificateStatus.ISSUED;
		}
		if (rs.getTime(2) != null
				&& rs.getString(3).equals("1970-01-01 00:00:00.0")) {
			return CertificateStatus.BEING_REVOKED;
		}
		return CertificateStatus.REVOKED;
	}

	public void issue() throws IOException {
		try {
			if (getStatus() != CertificateStatus.DRAFT) {
				throw new IllegalStateException();
			}
			PreparedStatement inserter = DatabaseConnection
					.getInstance()
					.prepare(
							"INSERT INTO emailcerts SET md=?, subject=?, coll_found=0, crt_name=''");
			inserter.setString(1, md);
			inserter.setString(2, dn);
			inserter.execute();
			id = DatabaseConnection.lastInsertId(inserter);
			File csrFile = KeyStorage.locateCsr(id);
			csrName = csrFile.getPath();
			FileOutputStream fos = new FileOutputStream(csrFile);
			fos.write(csr.getBytes());
			fos.close();

			PreparedStatement updater = DatabaseConnection.getInstance()
					.prepare("UPDATE emailcerts SET csr_name=? WHERE id=?");
			updater.setString(1, csrName);
			updater.setInt(2, id);
			updater.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public boolean waitFor(int max) throws SQLException, InterruptedException {
		long start = System.currentTimeMillis();
		while (getStatus().isUnstable()) {
			if (max != 0 && System.currentTimeMillis() - start > max) {
				return false;
			}
			Thread.sleep((long) (2000 + Math.random() * 2000));
		}
		return true;
	}
	public void revoke() {
		try {
			if (getStatus() != CertificateStatus.ISSUED) {
				throw new IllegalStateException();
			}
			PreparedStatement inserter = DatabaseConnection
					.getInstance()
					.prepare(
							"UPDATE emailcerts SET revoked = '1970-01-01' WHERE id=?");
			inserter.setInt(1, id);
			inserter.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public X509Certificate cert() throws IOException, GeneralSecurityException,
			SQLException {
		CertificateStatus status = getStatus();
		if (status != CertificateStatus.ISSUED) {
			throw new IllegalStateException(status + " is not wanted here.");
		}
		InputStream is = null;
		X509Certificate crt = null;
		try {
			is = new FileInputStream(crtName);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			crt = (X509Certificate) cf.generateCertificate(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return crt;
	}
	public Certificate renew() {
		return null;
	}
	public int getId() {
		return id;
	}
	public int getSerial() {
		return serial;
	}
	public String getDistinguishedName() {
		return dn;
	}
	public String getMessageDigest() {
		return md;
	}

}
