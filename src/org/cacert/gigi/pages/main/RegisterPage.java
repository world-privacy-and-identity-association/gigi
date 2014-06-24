package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.pages.Page;

public class RegisterPage extends Page {

	private static final String SIGNUP_PROCESS = "signupProcess";
	public static final String PATH = "/register";

	public RegisterPage() {
		super("Register");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		getDefaultTemplate().output(out, getLanguage(req),
				new HashMap<String, Object>());
		Signup s = getForm(req);
		s.writeForm(out, getLanguage(req));
	}
	public Signup getForm(HttpServletRequest req) {
		HttpSession hs = req.getSession();
		Signup s = (Signup) hs.getAttribute(SIGNUP_PROCESS);
		if (s == null) {
			s = new Signup();
			hs.setAttribute(SIGNUP_PROCESS, s);
		}
		return s;

	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Signup s = getForm(req);
		if (s.submit(resp.getWriter(), req)) {
			HttpSession hs = req.getSession();
			hs.setAttribute(SIGNUP_PROCESS, null);
			resp.getWriter()
					.println(
							translate(
									req,
									"Your information has been submitted"
											+ " into our system. You will now be sent an email with a web link,"
											+ " you need to open that link in your web browser within 24 hours"
											+ " or your information will be removed from our system!"));
			return;
		}

		super.doPost(req, resp);
	}
	@Override
	public boolean needsLogin() {
		return false;
	}
}
