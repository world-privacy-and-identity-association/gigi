package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.CertificateTable;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class MailCertificates extends Page {
	CertificateTable myTable = new CertificateTable("mailcerts");
	public static final String PATH = "/account/certs/email";

	public MailCertificates() {
		super("Email Certificates");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		String pi = req.getPathInfo().substring(PATH.length());
		if (pi.length() != 0) {
			pi = pi.substring(1);
			int id = Integer.parseInt(pi);
			Certificate c = new Certificate(id);
			// TODO check ownership
			out.println("<pre>");
			try {
				out.print(c.cert());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			out.println("</pre>");
			return;
		}

		HashMap<String, Object> vars = new HashMap<String, Object>();
		User us = LoginPage.getUser(req);
		try {
			PreparedStatement ps = DatabaseConnection
					.getInstance()
					.prepare(
							"SELECT `id`, `CN`, `serial`, `revoked`, `expire`, `disablelogin` FROM `emailcerts` WHERE `memid`=?");
			ps.setInt(1, us.getId());
			ResultSet rs = ps.executeQuery();
			vars.put("mailcerts", rs);
			myTable.output(out, getLanguage(req), vars);
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
