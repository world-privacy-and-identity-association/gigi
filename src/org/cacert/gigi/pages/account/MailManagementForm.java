package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;

public class MailManagementForm extends Form {
	private static Template t;
	private User target;
	static {
		t = new Template(ChangePasswordPage.class.getResource("MailManagementForm.templ"));
	}

	public MailManagementForm(HttpServletRequest hsr, User target) {
		super(hsr);
		this.target = target;
	}

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		if (req.getParameter("makedefault") != null) {
			try {
				target.updateDefaultEmail(EmailAddress.getById(Integer.parseInt(req.getParameter("emailid").trim())));
			} catch (Exception e) {
				out.println("<b>Error precessing your request.</b>");
				e.printStackTrace();
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
		t.output(out, l, vars);
	}

}
