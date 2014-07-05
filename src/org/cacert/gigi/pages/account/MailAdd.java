package org.cacert.gigi.pages.account;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.pages.Page;

public class MailAdd extends Page {
	public static final String DEFAULT_PATH = "/account/mails/add";

	public MailAdd(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

	}

}
