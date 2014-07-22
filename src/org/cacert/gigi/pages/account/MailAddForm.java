package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;

public class MailAddForm extends Form {
	private static Template t;
	static {
		t = new Template(ChangePasswordPage.class.getResource("MailAddForm.templ"));
	}

	public MailAddForm(HttpServletRequest hsr) {
		super(hsr);
	}

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		return false;
	}

	@Override
	protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
		t.output(out, l, vars);
	}

}
