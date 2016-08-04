package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class FindUserByEmailPage extends Page {

    public static final String PATH = "/support/find/email";

    public FindUserByEmailPage() {
        super("Find Email");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("first", true);
        new FindUserByEmailForm(req).output(resp.getWriter(), Page.getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FindUserByEmailForm form = Form.getForm(req, FindUserByEmailForm.class);
        try {
            form.submit(resp.getWriter(), req);
            final EmailAddress[] emails = form.getEmails();
            if (emails.length == 1) {
                resp.sendRedirect(SupportUserDetailsPage.PATH + emails[0].getOwner().getId() + "/");
            } else {
                HashMap<String, Object> vars = new HashMap<String, Object>();
                vars.put("first", false);
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
                form.output(resp.getWriter(), getLanguage(req), vars);
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), Page.getLanguage(req));
            doGet(req, resp);
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }

}
