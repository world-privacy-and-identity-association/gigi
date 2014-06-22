package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class MainPage extends Page {
	public MainPage(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, ServletResponse resp)
			throws IOException {
		resp.getWriter().println("Access granted.");
	}
}
