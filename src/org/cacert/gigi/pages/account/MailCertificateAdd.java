package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Certificate;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class MailCertificateAdd extends Page {
	public static final String PATH = "/account/certs/email/new";
	Template t = new Template(MailCertificateAdd.class.getResource("RequestCertificate.templ"));

	public MailCertificateAdd() {
		super("Create Email certificate");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("CCA", "<a href='/policy/CAcertCommunityAgreement.html'>CCA</a>");

		t.output(resp.getWriter(), getLanguage(req), vars);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		IssueCertificateForm f;
		if (req.getParameter(Form.CSRF_FIELD) != null) {
			f = Form.getForm(req, IssueCertificateForm.class);
			if (f.submit(resp.getWriter(), req)) {
				Certificate c = f.getResult();
				String ser = c.getSerial();
				resp.sendRedirect(MailCertificates.PATH + "/" + ser);
			}
		} else {
			f = new IssueCertificateForm(req);
			f.submit(resp.getWriter(), req);
		}
		f.output(resp.getWriter(), getLanguage(req), Collections.<String, Object> emptyMap());

	}
}
