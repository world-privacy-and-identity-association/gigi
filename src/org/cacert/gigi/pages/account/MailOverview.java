package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class MailOverview extends Page {

    public static final String DEFAULT_PATH = "/account/mails";

    public MailOverview(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final User us = getUser(req);
        Language lang = Page.getLanguage(req);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("addForm", new MailAddForm(req, us));
        vars.put("manForm", new MailManagementForm(req, us));
        getDefaultTemplate().output(resp.getWriter(), lang, vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        if (req.getParameter("addmail") != null) {
            MailAddForm f = Form.getForm(req, MailAddForm.class);
            if (f.submit(out, req)) {
                resp.sendRedirect(MailOverview.DEFAULT_PATH);
            }
        } else if (req.getParameter("makedefault") != null || req.getParameter("delete") != null) {
            MailManagementForm f = Form.getForm(req, MailManagementForm.class);
            if (f.submit(out, req)) {
                resp.sendRedirect(MailOverview.DEFAULT_PATH);
            }
        }
        super.doPost(req, resp);
    }

}
