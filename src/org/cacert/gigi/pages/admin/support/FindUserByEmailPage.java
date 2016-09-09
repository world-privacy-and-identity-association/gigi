package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class FindUserByEmailPage extends Page {

    public static final String PATH = "/support/find/email";

    private static final Template USERTABLE = new Template(FindUserByDomainPage.class.getResource("FindUserByEmailUsertable.templ"));

    public FindUserByEmailPage() {
        super("Find Email");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new FindUserByEmailForm(req).output(resp.getWriter(), Page.getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, FindUserByEmailForm.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form.getForm(req, FindUserByEmailForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } else {
            final EmailAddress[] emails = ((FindUserByEmailForm.FindEmailResult) req.getAttribute(Form.SUBMIT_RESULT)).getEmails();
            if (emails.length == 1) {
                resp.sendRedirect(SupportUserDetailsPage.PATH + emails[0].getOwner().getId() + "/");
            } else {
                HashMap<String, Object> vars = new HashMap<String, Object>();
                vars.put("usertable", new IterableDataset() {

                    int i = 0;

                    @Override
                    public boolean next(Language l, Map<String, Object> vars) {
                        if (i == emails.length) {
                            return false;
                        }
                        vars.put("usrid", emails[i].getOwner().getId());
                        vars.put("usermail", emails[i].getAddress());
                        i++;
                        return true;
                    }
                });
                USERTABLE.output(resp.getWriter(), getLanguage(req), vars);
            }
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }

}
