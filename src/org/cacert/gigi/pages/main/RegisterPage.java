package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		t.output(out, getLanguage(req), new HashMap<String, Object>());
		Signup s = getForm(req);
		s.writeForm(out, getLanguage(req));
	}
	public Signup getForm(HttpServletRequest req) {
		HttpSession hs = req.getSession();
		Signup s = (Signup) hs.getAttribute("signupProcess");
		if (s == null) {
			s = new Signup();
			hs.setAttribute("signupProcess", s);
		}
		return s;

	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Signup s = getForm(req);
		s.submit(resp.getWriter(), req);

		super.doPost(req, resp);
	}
	@Override
	public boolean needsLogin() {
		return false;
	}
}
