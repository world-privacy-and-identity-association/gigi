package org.cacert.gigi.pages.account.mail;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Form.CSRFException;
import org.cacert.gigi.pages.ManagedMultiFormPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class MailOverview extends ManagedMultiFormPage {

    public static final String DEFAULT_PATH = "/account/mails";

    public MailOverview() {
        super("Email addresses");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUser(req);
        output(req, resp, new MailAddForm(req, user), new MailManagementForm(req, user));
    }

    private void output(HttpServletRequest req, HttpServletResponse resp, MailAddForm addForm, MailManagementForm mgmtForm) throws IOException {
        Language lang = Page.getLanguage(req);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("addForm", addForm);
        vars.put("manForm", mgmtForm);
        getDefaultTemplate().output(resp.getWriter(), lang, vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Form current = getForm(req);
        if (Form.printFormErrors(req, resp.getWriter())) {
            User user = getUser(req);
            if (current instanceof MailAddForm) {
                output(req, resp, (MailAddForm) current, new MailManagementForm(req, user));
            } else {
                output(req, resp, new MailAddForm(req, user), (MailManagementForm) current);
            }
        }
    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        if (req.getParameter("addmail") != null) {
            return Form.getForm(req, MailAddForm.class);
        } else {
            return Form.getForm(req, MailManagementForm.class);
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getTarget() instanceof User;
    }

}
