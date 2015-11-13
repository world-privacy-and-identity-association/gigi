package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.util.AuthorizationContext;

public class PasswordResetPage extends Page {

    public static final String PATH = "/passwordReset";

    public PasswordResetPage() {
        super("Password Reset");
    }

    public static class PasswordResetForm extends Form {

        private static Template t = new Template(PasswordResetForm.class.getResource("PasswordResetForm.templ"));

        private User u;

        private int id;

        public PasswordResetForm(HttpServletRequest hsr) throws GigiApiException {
            super(hsr, PATH);
            id = Integer.parseInt(hsr.getParameter("id"));
            u = User.getResetWithToken(id, hsr.getParameter("token"));
            if (u == null) {
                throw new GigiApiException("User missing or token invalid");
            }

        }

        @Override
        public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
            String p1 = req.getParameter("pword1");
            String p2 = req.getParameter("pword2");
            String tok = req.getParameter("private_token");
            if (p1 == null || p2 == null || tok == null) {
                throw new GigiApiException("Missing form parameter.");
            }
            if ( !p1.equals(p2)) {
                throw new GigiApiException("New passwords differ.");
            }
            u.consumePasswordResetTicket(id, tok, p1);
            return true;
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {

            t.output(out, l, vars);
        }

    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PasswordResetForm form = Form.getForm(req, PasswordResetForm.class);
        try {
            form.submit(resp.getWriter(), req);
            resp.getWriter().println(getLanguage(req).getTranslation("Password reset successful."));
            return;
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
        form.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            new PasswordResetForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }
}
