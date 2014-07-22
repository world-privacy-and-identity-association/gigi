package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.GigiApiException;

public class Verify extends Page {
	public static final String PATH = "/verify";

	public Verify() {
		super("Verify email");
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		String hash = req.getParameter("hash");
		String type = req.getParameter("type");
		String id = req.getParameter("id");
		if ("email".equals(type)) {
			try {
				EmailAddress ea = EmailAddress.getById(Integer.parseInt(id));
				ea.verify(hash);
				out.println("Email verification completed.");
			} catch (IllegalArgumentException e) {
				out.println(translate(req, "The email address is invalid."));
			} catch (GigiApiException e) {
				e.format(out, getLanguage(req));
			}
		}
	}

}
