package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

public abstract class Form implements Outputable {
	String csrf;
	public Form() {
		csrf = RandomToken.generateToken(32);
	}

	public abstract boolean submit(PrintWriter out, HttpServletRequest req);
	@Override
	public final void output(PrintWriter out, Language l,
			Map<String, Object> vars) {
		out.println("<form method='POST' autocomplete='off'>");
		outputContent(out, l, vars);
		out.println("<input type='csrf' value='");
		out.print(getCSRFToken());
		out.println("'></form>");
	}

	public abstract void outputContent(PrintWriter out, Language l,
			Map<String, Object> vars);

	protected void outputError(PrintWriter out, ServletRequest req, String text) {
		out.print("<div>");
		out.print(Page.translate(req, text));
		out.println("</div>");
	}

	public String getCSRFToken() {
		return csrf;
	}

}
