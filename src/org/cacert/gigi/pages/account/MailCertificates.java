package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;

import javax.servlet.ServletOutputStream;
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
	public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String pi = req.getPathInfo().substring(PATH.length());
		if (pi.length() == 0) {
			return false;
		}
		pi = pi.substring(1);
		boolean crt = false;
		boolean cer = false;
		if (pi.endsWith(".crt")) {
			crt = true;
			pi = pi.substring(0, pi.length() - 4);
		} else if (pi.endsWith(".cer")) {
			cer = true;
			pi = pi.substring(0, pi.length() - 4);
		}
		int serial = 0;
		try {
			serial = Integer.parseInt(pi);
		} catch (NumberFormatException nfe) {
			resp.sendError(404);
			return true;
		}
		try {
			Certificate c = new Certificate(serial);
			if (LoginPage.getUser(req).getId() != c.getOwnerId()) {
				resp.sendError(404);
				return true;
			}
			X509Certificate cert = c.cert();
			if (!crt && !cer) {
				return false;
			}
			ServletOutputStream out = resp.getOutputStream();
			if (crt) {
				out.println("-----BEGIN CERTIFICATE-----");
				String block = Base64.getEncoder().encodeToString(cert.getEncoded()).replaceAll("(.{64})(?=.)", "$1\n");
				out.println(block);
				out.println("-----END CERTIFICATE-----");
			} else if (cer) {
				out.write(cert.getEncoded());
			}
		} catch (IllegalArgumentException e) {
			resp.sendError(404);
			return true;
		} catch (GeneralSecurityException e) {
			resp.sendError(404);
			return true;
		} catch (SQLException e) {
			resp.sendError(404);
			return true;
		}

		return true;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		String pi = req.getPathInfo().substring(PATH.length());
		if (pi.length() != 0) {
			pi = pi.substring(1);

			int serial = 0;
			try {
				serial = Integer.parseInt(pi);
			} catch (NumberFormatException nfe) {
			}
			Certificate c = null;
			if (serial != 0) {
				c = new Certificate(serial);
			}
			if (c == null || LoginPage.getUser(req).getId() != c.getOwnerId()) {
				resp.sendError(404);
				return;
			}
			out.print("<a href='");
			out.print(serial);
			out.print(".crt'>");
			out.print(translate(req, "PEM encoded Certificate"));
			out.println("</a><br/>");

			out.print("<a href='");
			out.print(serial);
			out.print(".cer'>");
			out.print(translate(req, "DER encoded Certificate"));
			out.println("</a><br/>");

			out.println("<pre>");
			try {
				X509Certificate cert = c.cert();
				out.print(cert);
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
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
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
