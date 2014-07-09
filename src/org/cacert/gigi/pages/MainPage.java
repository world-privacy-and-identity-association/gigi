package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MainPage extends Page {
	public MainPage(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.getWriter().println("Access granted.");
	}

	@Override
	public boolean needsLogin() {
		return false;
	}
}
