package org.cacert.gigi.pages.account;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.Template;

public class ChangeForm extends Form {
	private static Template t;
	static {
		t = new Template(new InputStreamReader(
				ChangePasswordPage.class
						.getResourceAsStream("ChangePasswordForm.templ")));
	}

	@Override
	public void outputContent(PrintWriter out, Language l,
			Map<String, Object> vars) {
		t.output(out, l, vars);
	}

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		return false;
	}

}
