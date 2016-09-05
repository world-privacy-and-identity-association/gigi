package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.RateLimit;

public class RegisterPage extends Page {

    private static final String SIGNUP_PROCESS = "signupProcess";

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
        PrintWriter out = resp.getWriter();
        HashMap<String, Object> vars = new HashMap<String, Object>();
        getDefaultTemplate().output(out, getLanguage(req), vars);
        s.output(out, getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Signup s = Form.getForm(req, Signup.class);
        try {
            if (s.submit(req)) {
                HttpSession hs = req.getSession();
                hs.setAttribute(SIGNUP_PROCESS, null);
                resp.getWriter().println(translate(req, "Your information has been submitted" + " into our system. You will now be sent an email with a web link," + " you need to open that link in your web browser within 24 hours" + " or your information will be removed from our system!"));
                return;
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }

        outputGet(req, resp, s);
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
