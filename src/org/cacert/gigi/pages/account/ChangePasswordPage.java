package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class ChangePasswordPage extends Page {

    public static final String PATH = "/account/password";

    public ChangePasswordPage() {
        super("Change Password");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new ChangeForm(req, getUser(req)).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Form.getForm(req, ChangeForm.class).submitProtected(resp.getWriter(), req);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getTarget() instanceof User;
    }
}
