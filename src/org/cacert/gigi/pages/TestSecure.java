package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestSecure extends Page {

	public TestSecure() {
		super("Secure testpage");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.getWriter().println("This page is secure.");
	}

}
