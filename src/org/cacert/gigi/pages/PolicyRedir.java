package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PolicyRedir extends Page {
	public PolicyRedir() {
		super("Policy");
	}

	public static final String PATH = "/policy/*";
	@Override
	public boolean beforeTemplate(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		String substring = req.getPathInfo().substring(PATH.length() - 1);
		resp.sendRedirect("/static/policy/"
				+ substring.replace(".php", ".html"));
		return true;
	}
	@Override
	public boolean needsLogin() {
		return false;
	}
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

	}

}
