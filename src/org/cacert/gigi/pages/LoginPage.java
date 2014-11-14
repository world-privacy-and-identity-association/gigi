package org.cacert.gigi.pages;

import static org.cacert.gigi.Gigi.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.util.PasswordHash;

public class LoginPage extends Page {

    public class LoginForm extends Form {

        public LoginForm(HttpServletRequest hsr) {
            super(hsr);
        }

        @Override
        public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
            tryAuthWithUnpw(req);
            return false;
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
            getDefaultTemplate().output(out, l, vars);
        }

    }

    public static final String LOGIN_RETURNPATH = "login-returnpath";

    public LoginPage(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new LoginForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redir = (String) req.getSession().getAttribute(LOGIN_RETURNPATH);
        if (req.getSession().getAttribute("loggedin") == null) {
            X509Certificate[] cert = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
            if (cert != null && cert[0] != null) {
                tryAuthWithCertificate(req, cert[0]);
            }
            if (req.getMethod().equals("POST")) {
                try {
                    Form.getForm(req, LoginForm.class).submit(resp.getWriter(), req);
                } catch (GigiApiException e) {
                }
            }
        }

        if (req.getSession().getAttribute("loggedin") != null) {
            String s = redir;
            if (s != null) {
                if ( !s.startsWith("/")) {
                    s = "/" + s;
                }
                resp.sendRedirect(s);
            } else {
                resp.sendRedirect("/");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    private void tryAuthWithUnpw(HttpServletRequest req) {
        String un = req.getParameter("username");
        String pw = req.getParameter("password");
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `password`, `id` FROM `users` WHERE `email`=? AND verified='1'");
        ps.setString(1, un);
        GigiResultSet rs = ps.executeQuery();
        if (rs.next()) {
            if (PasswordHash.verifyHash(pw, rs.getString(1))) {
                loginSession(req, User.getById(rs.getInt(2)));
            }
        }
        rs.close();
    }

    public static User getUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute(USER);
    }

    private void tryAuthWithCertificate(HttpServletRequest req, X509Certificate x509Certificate) {
        String serial = x509Certificate.getSerialNumber().toString(16).toUpperCase();
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `memid` FROM `certs` WHERE `serial`=? AND `disablelogin`='0' AND `revoked` is NULL");
        ps.setString(1, serial);
        GigiResultSet rs = ps.executeQuery();
        if (rs.next()) {
            loginSession(req, User.getById(rs.getInt(1)));
            req.getSession().setAttribute(CERT_SERIAL, serial);
            req.getSession().setAttribute(CERT_ISSUER, x509Certificate.getIssuerDN());
        }
        rs.close();
    }

    private static final Group LOGIN_BLOCKED = Group.getByString("blockedlogin");

    private void loginSession(HttpServletRequest req, User user) {
        if (user.isInGroup(LOGIN_BLOCKED)) {
            return;
        }
        req.getSession().invalidate();
        HttpSession hs = req.getSession();
        hs.setAttribute(LOGGEDIN, true);
        hs.setAttribute(Language.SESSION_ATTRIB_NAME, user.getPreferredLocale());
        hs.setAttribute(USER, user);
    }

    @Override
    public boolean isPermitted(User u) {
        return u == null;
    }
}
