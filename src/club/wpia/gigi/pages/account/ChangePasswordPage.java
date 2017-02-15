package club.wpia.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.util.AuthorizationContext;

public class ChangePasswordPage extends ManagedFormPage {

    public static final String PATH = "/account/password";

    public ChangePasswordPage() {
        super("Change Password", ChangeForm.class);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new ChangeForm(req, getUser(req)).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getTarget() instanceof User;
    }
}
