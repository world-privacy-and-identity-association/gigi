package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.output.ClientCSRGenerate;
import org.cacert.gigi.pages.Page;

public class MailCertificateAdd extends Page {
	public static final String PATH = "/account/certs/email/new";

	public MailCertificateAdd() {
		super("Create Email certificate");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("CCA",
				"<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");
		getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		if (req.getParameter("optionalCSR") == null
				|| req.getParameter("optionalCSR").equals("")) {
			out.println("csr missing");
			ClientCSRGenerate.output(req, resp);
		}
		String csr = req.getParameter("optionalCSR");
		if (!"on".equals(req.getParameter("CCA"))) {
			// Error.
			return;
		}
		Certificate c = new Certificate("/commonName=felix@dogcraft.de",
				"sha256", csr);
		c.issue();
		try {
			c.waitFor(60000);
			out.println(c.getStatus());
			out.println(c.cert());
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
