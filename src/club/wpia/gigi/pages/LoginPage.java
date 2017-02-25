package club.wpia.gigi.pages;

import static club.wpia.gigi.Gigi.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.main.RegisterPage;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.PasswordHash;
import club.wpia.gigi.util.RateLimit;
import club.wpia.gigi.util.RateLimit.RateLimitException;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class LoginPage extends Page {

    public static final RateLimit RATE_LIMIT = new RateLimit(10, 5 * 60 * 1000);

    public class LoginForm extends Form {

        public LoginForm(HttpServletRequest hsr) {
            super(hsr);
        }

        @Override
        public RedirectResult submit(HttpServletRequest req) throws GigiApiException {
            if (RegisterPage.RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
                throw new RateLimitException();
            }
            tryAuthWithUnpw(req);
            return new RedirectResult(redirectPath(req));
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
            getDefaultTemplate().output(out, l, vars);
        }

    }

    public static final String LOGIN_RETURNPATH = "login-returnpath";

    public LoginPage() {
        super("Password Login");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getHeader("Host").equals(ServerConstants.getHostNamePortSecure(Host.SECURE))) {
            resp.getWriter().println(getLanguage(req).getTranslation("Authentication with certificate failed. Try another certificate or use a password."));
        } else {
            new LoginForm(req).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form.getForm(req, LoginForm.class).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getSession().getAttribute("loggedin") == null) {
            X509Certificate cert = getCertificateFromRequest(req);
            if (cert != null) {
                tryAuthWithCertificate(req, cert);
            }
            if (req.getMethod().equals("POST")) {
                return Form.getForm(req, LoginForm.class).submitExceptionProtected(req, resp);
            }
        }

        if (req.getSession().getAttribute("loggedin") != null) {
            resp.sendRedirect(redirectPath(req));
            return true;
        }
        return false;
    }

    private static String redirectPath(HttpServletRequest req) {
        String redir = (String) req.getAttribute(LOGIN_RETURNPATH);
        String s = redir;
        if (s != null) {
            if ( !s.startsWith("/")) {
                s = "/" + s;
            }
            return s;
        } else {
            return "/";
        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    private void tryAuthWithUnpw(HttpServletRequest req) throws GigiApiException {
        String un = req.getParameter("username");
        String pw = req.getParameter("password");
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `password`, `id` FROM `users` WHERE `email`=? AND verified='1'")) {
            ps.setString(1, un);
            GigiResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbHash = rs.getString(1);
                String hash = PasswordHash.verifyHash(pw, dbHash);
                if (hash != null) {
                    if ( !hash.equals(dbHash)) {
                        try (GigiPreparedStatement gps = new GigiPreparedStatement("UPDATE `users` SET `password`=? WHERE `email`=?")) {
                            gps.setString(1, hash);
                            gps.setString(2, un);
                            gps.executeUpdate();
                        }
                    }
                    loginSession(req, User.getById(rs.getInt(2)));
                    req.getSession().setAttribute(LOGIN_METHOD, new TranslateCommand("Password"));
                    return;
                }
            }
        }
        throw new GigiApiException("Username and password didn't match.");
    }

    public static User getUser(HttpServletRequest req) {
        AuthorizationContext ac = getAuthorizationContext(req);
        if (ac == null) {
            return null;
        }
        return ac.getActor();
    }

    public static AuthorizationContext getAuthorizationContext(HttpServletRequest req) {
        return ((AuthorizationContext) req.getSession().getAttribute(AUTH_CONTEXT));
    }

    private void tryAuthWithCertificate(HttpServletRequest req, X509Certificate x509Certificate) {
        String serial = extractSerialFormCert(x509Certificate);
        User user = fetchUserBySerial(serial);
        if (user == null) {
            return;
        }
        loginSession(req, user);
        req.getSession().setAttribute(CERT_SERIAL, serial);
        req.getSession().setAttribute(CERT_ISSUER, x509Certificate.getIssuerDN());
        req.getSession().setAttribute(LOGIN_METHOD, new TranslateCommand("Certificate"));
    }

    public static String extractSerialFormCert(X509Certificate x509Certificate) {
        return x509Certificate.getSerialNumber().toString(16).toLowerCase();
    }

    public static User fetchUserBySerial(String serial) {
        if ( !serial.matches("[a-f0-9]+")) {
            throw new Error("serial malformed.");
        }

        CertificateOwner o = CertificateOwner.getByEnabledSerial(serial);
        if (o == null || !(o instanceof User)) {
            return null;
        }
        return (User) o;
    }

    public static X509Certificate getCertificateFromRequest(HttpServletRequest req) {
        X509Certificate[] cert = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        X509Certificate uc = null;
        if (cert != null && cert[0] != null) {
            uc = cert[0];
        }
        return uc;
    }

    private static final Group LOGIN_BLOCKED = Group.BLOCKED_LOGIN;

    private void loginSession(HttpServletRequest req, User user) {
        if (user.isInGroup(LOGIN_BLOCKED)) {
            return;
        }
        req.setAttribute(LOGIN_RETURNPATH, req.getSession().getAttribute(LOGIN_RETURNPATH));
        req.getSession().invalidate();
        HttpSession hs = req.getSession();
        hs.setAttribute(LOGGEDIN, true);
        hs.setAttribute(Language.SESSION_ATTRIB_NAME, user.getPreferredLocale());
        hs.setAttribute(AUTH_CONTEXT, new AuthorizationContext(user, user));
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac == null;
    }
}
