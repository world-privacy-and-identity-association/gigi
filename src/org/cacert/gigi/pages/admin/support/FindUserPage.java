package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class FindUserPage extends Page {

    public static final String PATH = "/support/find/user";

    public FindUserPage() {
        super("Find User");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("first", true);
        new FindUserForm(req).output(resp.getWriter(), Page.getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FindUserForm form = Form.getForm(req, FindUserForm.class);
        try {
            form.submit(resp.getWriter(), req);
            final User[] users = form.getUsers();
            if (users.length == 1) {
                resp.sendRedirect(SupportUserDetailsPage.PATH + users[0].getId());
            } else {
                HashMap<String, Object> vars = new HashMap<String, Object>();
                vars.put("first", false);
                vars.put("usertable", new IterableDataset() {

                    int i = 0;

                    @Override
                    public boolean next(Language l, Map<String, Object> vars) {
                        if (i == users.length) {
                            return false;
                        }
                        vars.put("usrid", users[i].getId());
                        vars.put("usermail", users[i].getEmail());
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
