package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Template;

public abstract class Page {
	private String title;
	private Template defaultTemplate;

	public Page(String title) {
		this.title = title;
		try {
			InputStream resource = getClass().getResourceAsStream(
					getClass().getSimpleName() + ".templ");
			if (resource != null) {
				defaultTemplate = new Template(new InputStreamReader(resource,
						"UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	public Template getDefaultTemplate() {
		return defaultTemplate;
	}

	public boolean beforeTemplate(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		return false;
	}
	public abstract void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
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
	public boolean needsLogin() {
		return true;
	}

}
