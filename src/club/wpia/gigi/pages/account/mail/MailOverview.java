package club.wpia.gigi.pages.account.mail;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Form.CSRFException;
import club.wpia.gigi.pages.ManagedMultiFormPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

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
