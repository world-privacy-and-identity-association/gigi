package club.wpia.gigi.pages.main;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.RateLimit;

public class RegisterPage extends Page {

    public static final String PATH = "/register";

    // 50 per 5 min
    public static final RateLimit RATE_LIMIT = new RateLimit(50, 5 * 60 * 1000);

    public RegisterPage() {
        super("Register");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Signup s = new Signup(req);
        outputGet(req, resp, s);
    }

    private void outputGet(HttpServletRequest req, HttpServletResponse resp, Signup s) throws IOException {
        Map<String, Object> vars = getDefaultVars(req);
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
        s.output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, Signup.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Signup s = Form.getForm(req, Signup.class);
            outputGet(req, resp, s);
        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac == null;
    }
}
