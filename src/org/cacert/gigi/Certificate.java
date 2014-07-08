package org.cacert.gigi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.cacert.gigi.database.DatabaseConnection;

public class Certificate {
	int id;
	int serial;
	String dn;
	String md;
	String csrName;
	String crtName;

	// created, modified, revoked, expire
	public enum CertificateStatus {
		DRAFT(false), BEEING_ISSUED(true), ISSUED(false), BEEING_REVOKED(true), REVOKED(
				false);

		boolean unstable;

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
			return CertificateStatus.BEEING_ISSUED;
		}
		crtName = rs.getString(1);
		if (rs.getTime(2) != null && rs.getTime(3) == null) {
			return CertificateStatus.ISSUED;
		}
		if (rs.getTime(2) != null
				&& rs.getString(3).equals("1970-01-01 00:00:00.0")) {
			return CertificateStatus.BEEING_REVOKED;
		}
		return CertificateStatus.REVOKED;
	}

	public void issue() {
		try {
			if (getStatus() != CertificateStatus.DRAFT) {
				throw new IllegalStateException();
			}
			PreparedStatement inserter = DatabaseConnection
					.getInstance()
					.prepare(
							"INSERT INTO emailcerts SET csr_name =?, md=?, subject=?, coll_found=0, crt_name=''");
			inserter.setString(1, csrName);
			inserter.setString(2, md);
			inserter.setString(3, dn);
			inserter.execute();
			id = DatabaseConnection.lastInsertId(inserter);
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

	public X509Certificate cert() throws IOException, GeneralSecurityException {
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

}
