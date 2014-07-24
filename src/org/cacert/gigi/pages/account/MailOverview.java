package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class MailOverview extends Page {
	public static final String DEFAULT_PATH = "/account/mails";
	private MailTable t;

	public MailOverview(String title) {
		super(title);
		t = new MailTable("us");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final User us = LoginPage.getUser(req);
		Language lang = Page.getLanguage(req);
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("mailData", t);
		vars.put("us", us);
		vars.put("addForm", new MailAddForm(req, us));
		vars.put("manForm", new MailManagementForm(req));
		getDefaultTemplate().output(resp.getWriter(), lang, vars);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		User us = LoginPage.getUser(req);
		if (req.getParameter("addmail") != null) {
			MailAddForm f = Form.getForm(req, MailAddForm.class);
			if (f.submit(out, req)) {
				resp.sendRedirect(MailOverview.DEFAULT_PATH);
			}
		} else if (req.getParameter("makedefault") != null || req.getParameter("delete") != null) {
			System.out.println("MakeDefault/Delete");
			MailManagementForm f = Form.getForm(req, MailManagementForm.class);
			f.submit(out, req);
		}
		super.doPost(req, resp);
	}

	private class MailTable implements Outputable {
		private String user;

		public MailTable(String user) {
			this.user = user;
		}

		@Override
		public void output(PrintWriter out, Language l, Map<String, Object> vars) {
			User us = (User) vars.get(user);
			String usM = us.getEmail();
			EmailAddress[] emails = us.getEmails();

			for (int i = 0; i < emails.length; i++) {
				out.println("<tr>");
				out.println("<td><input type=\"radio\" name=\"emailid\" value=\"");
				int mailID = emails[i].getId();
				out.print(mailID);
				out.print("\" ");
				if (emails[i].getAddress().equals(us.getEmail())) {
					out.print("checked=\"yes\"");
				}
				out.print("/></td>");
				out.println("<td>");
				if (emails[i].isVerified()) {
					out.print(l.getTranslation("Verified"));
				} else {
					out.print(l.getTranslation("Unverified"));
				}
				out.print("</td>");
				out.println("<td>");
				String address = emails[i].getAddress();
				if (usM.equals(address)) {
					out.print(l.getTranslation("N/A"));
				} else {
					out.print("<input type=\"checkbox\" name=\"delid[]\" value=\"");
					out.print(mailID);
					out.print("\"/>");
				}
				out.print("</td>");
				out.println("<td>");
				out.print(address);
				out.print("</td>");
				out.println("</tr>");
			}
		}
	}
}
