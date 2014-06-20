package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.cacert.gigi.Page;

public class MainPage extends Page {
	@Override
	public void doGet(ServletRequest req, ServletResponse resp)
			throws IOException {
		super.doGet(req, resp);
		resp.getWriter().println("Access granted.");
	}
}
