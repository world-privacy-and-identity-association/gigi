package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class MainPage extends Page {
	public MainPage(String title) {
		super(title);
	}

	@Override
	public void doGet(ServletRequest req, ServletResponse resp)
			throws IOException {
		super.doGet(req, resp);
		resp.getWriter().println("Access granted.");
	}
}
