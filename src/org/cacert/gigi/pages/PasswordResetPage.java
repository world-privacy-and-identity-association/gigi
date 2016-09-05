package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.MailTemplate;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.HTMLEncoder;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;

public class PasswordResetPage extends Page {

    public static final int HOUR_MAX = 96;

    public static final String PATH = "/passwordReset";

    public PasswordResetPage() {
        super("Password Reset");
    }

    public static class PasswordResetForm extends Form {

        private static final Template t = new Template(PasswordResetForm.class.getResource("PasswordResetForm.templ"));

        private User u;

        private int id;

        public PasswordResetForm(HttpServletRequest hsr) throws GigiApiException {
            super(hsr, PATH);
            String idS = hsr.getParameter("id");
            String tokS = hsr.getParameter("token");
            if (idS == null || tokS == null) {
                throw new GigiApiException("requires id and token");
            }
            try {
                id = Integer.parseInt(idS);
            } catch (NumberFormatException e) {
                throw new GigiApiException("requires id to be integer");
            }
            u = User.getResetWithToken(id, tokS);
            if (u == null) {
                throw new GigiApiException("User missing or token invalid");
            }

        }

        @Override
        public boolean submit(HttpServletRequest req) throws GigiApiException {
            try (GigiPreparedStatement passwordReset = new GigiPreparedStatement("UPDATE `passwordResetTickets` SET `used` = CURRENT_TIMESTAMP WHERE `used` IS NULL AND `created` < CURRENT_TIMESTAMP - interval '1 hours' * ?;")) {
                passwordReset.setInt(1, HOUR_MAX);
                passwordReset.execute();
            }

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
        PrintWriter w = resp.getWriter();
        if (form.submitProtected(w, req)) {
            w.println("<div class='alert alert-success'>");
            w.println(HTMLEncoder.encodeHTML(getLanguage(req).getTranslation("Password reset successful.")));
            w.println("</div>");
            return;
        }
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

    private static final MailTemplate passwordResetMail = new MailTemplate(PasswordResetPage.class.getResource("PasswordResetMail.templ"));

    public static void initPasswordResetProcess(User targetUser, HttpServletRequest req, String aword, Language l, String method, String subject) {
        String ptok = RandomToken.generateToken(32);
        int id = targetUser.generatePasswordResetTicket(Page.getUser(req), ptok, aword);
        try {
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("subject", subject);
            vars.put("method", method);
            vars.put("link", "https://" + ServerConstants.getWwwHostNamePortSecure() + PasswordResetPage.PATH //
                    + "?id=" + id + "&token=" + URLEncoder.encode(ptok, "UTF-8"));
            vars.put("hour_max", HOUR_MAX);

            passwordResetMail.sendMail(l, vars, Page.getUser(req).getEmail());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
