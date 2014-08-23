package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class RegisterPage extends Page {

    private static final String SIGNUP_PROCESS = "signupProcess";

    public static final String PATH = "/register";

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
        if (s == null) {
            resp.getWriter().println(translate(req, "CSRF token check failed."));
        } else if (s.submit(resp.getWriter(), req)) {
            HttpSession hs = req.getSession();
            hs.setAttribute(SIGNUP_PROCESS, null);
            resp.getWriter().println(translate(req, "Your information has been submitted" + " into our system. You will now be sent an email with a web link," + " you need to open that link in your web browser within 24 hours" + " or your information will be removed from our system!"));
            return;
        }

        outputGet(req, resp, s);
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public boolean isPermitted(User u) {
        return u == null;
    }
}
