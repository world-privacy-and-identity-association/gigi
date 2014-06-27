package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.output.MailTable;
import org.cacert.gigi.pages.Page;

public class MailOverview extends Page {
	public static final String DEFAULT_PATH = "/account/mails";

	public MailOverview(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		HashMap<String, Object> vars = new HashMap<String, Object>();

		new MailTable().output(resp.getWriter(), getLanguage(req), vars);

	}

}
