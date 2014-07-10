package org.cacert.gigi.pages.error;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.pages.Page;

public class PageNotFound extends Page {

	public PageNotFound() {
		super("");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		getDefaultTemplate().output(resp.getWriter(), Page.getLanguage(req), null);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

}
