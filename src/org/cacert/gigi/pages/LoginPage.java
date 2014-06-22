package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class LoginPage extends Page {
	public LoginPage(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, ServletResponse resp)
			throws IOException {
		resp.getWriter()
				.println(
						"<form method='POST' action='/login'>"
								+ "<input type='text' name='username'>"
								+ "<input type='password' name='password'> <input type='submit' value='login'></form>");
	}

}
