package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

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
				String mailid = req.getParameter("emailid");
				if (mailid == null) {
					return false;
				}
				target.updateDefaultEmail(EmailAddress.getById(Integer.parseInt(mailid.trim())));
			} catch (GigiApiException e) {
				e.format(out, Page.getLanguage(req));
				e.printStackTrace();
				return false;
			}
			return true;
		}
		if (req.getParameter("delete") != null) {
			String[] toDel = req.getParameterValues("delid[]");
			if (toDel == null) {
				return false;
			}
			for (int i = 0; i < toDel.length; i++) {
				try {
					target.deleteEmail(EmailAddress.getById(Integer.parseInt(toDel[i].trim())));
				} catch (GigiApiException e) {
					e.format(out, Page.getLanguage(req));
					e.printStackTrace();
					return false;
				}
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
