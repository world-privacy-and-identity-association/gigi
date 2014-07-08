package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		if (req.getParameter("optionalCSR") == null
				|| req.getParameter("optionalCSR").equals("")) {
			out.println("csr missing");
		}
		out.println("could now start processing the cert request");
	}

}
