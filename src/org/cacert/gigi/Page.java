package org.cacert.gigi;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class Page {
	public void doGet(ServletRequest req, ServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
	}

	public void doPost(ServletRequest req, ServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}
}
