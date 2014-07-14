package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.ClientCSRGenerate;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class MailCertificateAdd extends Page {
	public static final String PATH = "/account/certs/email/new";

	public MailCertificateAdd() {
		super("Create Email certificate");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("CCA", "<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");

		User u = LoginPage.getUser(req);
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT `id`,`email` from `email` WHERE `memid`=? AND `deleted`=0");
			ps.setInt(1, u.getId());
			final ResultSet rs = ps.executeQuery();
			vars.put("emails", new IterableDataset() {

				@Override
				public boolean next(Language l, Map<String, Object> vars) {
					try {
						if (!rs.next()) {
							return false;
						}
						vars.put("id", rs.getString(1));
						vars.put("value", rs.getString(2));
						return true;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return false;
				}
			});
			getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		if (req.getParameter("optionalCSR") == null || req.getParameter("optionalCSR").equals("")) {
			out.println("csr missing");
			ClientCSRGenerate.output(req, resp);
		}
		String csr = req.getParameter("optionalCSR");
		if (!"on".equals(req.getParameter("CCA"))) {
			// Error.
			return;
		}
		Certificate c = new Certificate(LoginPage.getUser(req).getId(), "/commonName=CAcert WoT User", "sha256", csr);
		c.issue();
		try {
			c.waitFor(60000);
			resp.sendRedirect(MailCertificates.PATH + "/" + c.getSerial());
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
