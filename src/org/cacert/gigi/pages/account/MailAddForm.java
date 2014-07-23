package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;

public class MailAddForm extends Form {
	private static Template t;
	private String mail;
	static {
		t = new Template(ChangePasswordPage.class.getResource("MailAddForm.templ"));
	}

	public MailAddForm(HttpServletRequest hsr) {
		super(hsr);
	}

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		String formMail = req.getParameter("newemail");
		if (!EmailProvider.MAIL.matcher(formMail).matches()) {
			// TODO Proper error output (css, maybe abstract)
			out.println("<b>Error: Invalid address!</b>");
			return false;
		}
		mail = formMail;
		return true;
	}

	public String getMail() {
		return mail;
	}

	@Override
	protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
		t.output(out, l, vars);
	}

}
