package org.cacert.gigi;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class LoginPage extends Page {
	@Override
	public void doGet(ServletRequest req, ServletResponse resp)
			throws IOException {
		super.doGet(req, resp);
		resp.getWriter()
				.println(
						"<form method='POST' action='/login'>"
				+ "<input type='text' name='username'>"
				+ "<input type='password' name='password'> <input type='submit' value='login'></form>");
	}

}
