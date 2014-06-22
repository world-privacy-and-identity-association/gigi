package org.cacert.gigi.pages;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;

public abstract class Page {
	private String title;

	public Page(String title) {
		this.title = title;
	}

	public abstract void doGet(HttpServletRequest req, ServletResponse resp)
			throws IOException;

	public void doPost(HttpServletRequest req, ServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	public static Language getLanguage(ServletRequest req) {
		return Language.getInstance("de");
	}

	public static String translate(ServletRequest req, String string) {
		Language l = getLanguage(req);
		return l.getTranslation(string);
	}


}
