package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.pages.Page;

public class RegisterPage extends Page {

	public static final String PATH = "/register";
	Template t;

	public RegisterPage() {
		super("Register");
		try {
			t = new Template(new InputStreamReader(
					Signup.class.getResourceAsStream("RegisterPage.templ"),
					"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doGet(HttpServletRequest req, ServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		t.output(out, getLanguage(req), new HashMap<String, Object>());
		Signup s = new Signup();
		s.writeForm(out, req);
	}
	@Override
	public void doPost(HttpServletRequest req, ServletResponse resp)
			throws IOException {

		super.doPost(req, resp);
	}
}
