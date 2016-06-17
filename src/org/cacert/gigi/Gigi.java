package org.cacert.gigi;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.DatabaseConnection.Link;
import org.cacert.gigi.dbObjects.CACertificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Menu;
import org.cacert.gigi.output.MenuCollector;
import org.cacert.gigi.output.PageMenuItem;
import org.cacert.gigi.output.SimpleMenuItem;
import org.cacert.gigi.output.template.Form.CSRFException;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.HandlesMixedRequest;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.LogoutPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.pages.RootCertPage;
import org.cacert.gigi.pages.StaticPage;
import org.cacert.gigi.pages.TestSecure;
import org.cacert.gigi.pages.Verify;
import org.cacert.gigi.pages.account.ChangePasswordPage;
import org.cacert.gigi.pages.account.History;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.pages.account.UserTrainings;
import org.cacert.gigi.pages.account.certs.CertificateAdd;
import org.cacert.gigi.pages.account.certs.Certificates;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.pages.account.mail.MailOverview;
import org.cacert.gigi.pages.admin.TTPAdminPage;
import org.cacert.gigi.pages.admin.support.FindDomainPage;
import org.cacert.gigi.pages.admin.support.FindUserPage;
import org.cacert.gigi.pages.admin.support.SupportEnterTicketPage;
import org.cacert.gigi.pages.admin.support.SupportUserDetailsPage;
import org.cacert.gigi.pages.error.AccessDenied;
import org.cacert.gigi.pages.error.PageNotFound;
import org.cacert.gigi.pages.main.RegisterPage;
import org.cacert.gigi.pages.orga.CreateOrgPage;
import org.cacert.gigi.pages.orga.ViewOrgPage;
import org.cacert.gigi.pages.wot.AssurePage;
import org.cacert.gigi.pages.wot.MyListingPage;
import org.cacert.gigi.pages.wot.MyPoints;
import org.cacert.gigi.pages.wot.RequestTTPPage;
import org.cacert.gigi.ping.PingerDaemon;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.ServerConstants;

public final class Gigi extends HttpServlet {

    private class MenuBuilder {

        private LinkedList<Menu> categories = new LinkedList<Menu>();

        private HashMap<String, Page> pages = new HashMap<String, Page>();

        private MenuCollector rootMenu;

        public MenuBuilder() {}

        private void putPage(String path, Page p, String category) {
            pages.put(path, p);
            if (category == null) {
                return;
            }
            Menu m = getMenu(category);
            m.addItem(new PageMenuItem(p, path.replaceFirst("/?\\*$", "")));

        }

        private Menu getMenu(String category) {
            Menu m = null;
            for (Menu menu : categories) {
                if (menu.getMenuName().equals(category)) {
                    m = menu;
                    break;
                }
            }
            if (m == null) {
                m = new Menu(category);
                categories.add(m);
            }
            return m;
        }

        public MenuCollector generateMenu() throws ServletException {
            putPage("/denied", new AccessDenied(), null);
            putPage("/error", new PageNotFound(), null);
            putPage("/login", new LoginPage(), null);
            getMenu("SomeCA.org").addItem(new SimpleMenuItem("https://" + ServerConstants.getWwwHostNamePort() + "/login", "Password Login") {

                @Override
                public boolean isPermitted(AuthorizationContext ac) {
                    return ac == null;
                }
            });
            getMenu("SomeCA.org").addItem(new SimpleMenuItem("https://" + ServerConstants.getSecureHostNamePort() + "/login", "Certificate Login") {

                @Override
                public boolean isPermitted(AuthorizationContext ac) {
                    return ac == null;
                }
            });
            putPage("/", new MainPage(), null);
            putPage("/roots", new RootCertPage(truststore), "SomeCA.org");

            putPage("/secure", new TestSecure(), null);
            putPage(Verify.PATH, new Verify(), null);
            putPage(Certificates.PATH + "/*", new Certificates(), "Certificates");
            putPage(RegisterPage.PATH, new RegisterPage(), "SomeCA.org");
            putPage(CertificateAdd.PATH, new CertificateAdd(), "Certificates");
            putPage(MailOverview.DEFAULT_PATH, new MailOverview(), "Certificates");
            putPage(DomainOverview.PATH + "*", new DomainOverview(), "Certificates");

            putPage(AssurePage.PATH + "/*", new AssurePage(), "Web of Trust");
            putPage(MyPoints.PATH, new MyPoints(), "Web of Trust");
            putPage(MyListingPage.PATH, new MyListingPage(), "Web of Trust");
            putPage(RequestTTPPage.PATH, new RequestTTPPage(), "Web of Trust");

            putPage(TTPAdminPage.PATH + "/*", new TTPAdminPage(), "Admin");
            putPage(CreateOrgPage.DEFAULT_PATH, new CreateOrgPage(), "Organisation Admin");
            putPage(ViewOrgPage.DEFAULT_PATH + "/*", new ViewOrgPage(), "Organisation Admin");

            putPage(SupportEnterTicketPage.PATH, new SupportEnterTicketPage(), "Support Console");
            putPage(FindUserPage.PATH, new FindUserPage(), "Support Console");
            putPage(FindDomainPage.PATH, new FindDomainPage(), "Support Console");

            putPage(SupportUserDetailsPage.PATH + "*", new SupportUserDetailsPage(), null);
            putPage(ChangePasswordPage.PATH, new ChangePasswordPage(), "My Account");
            putPage(LogoutPage.PATH, new LogoutPage(), "My Account");
            putPage(History.PATH, new History(false), "My Account");
            putPage(History.SUPPORT_PATH, new History(true), null);
            putPage(UserTrainings.PATH, new UserTrainings(false), "My Account");
            putPage(MyDetails.PATH, new MyDetails(), "My Account");
            putPage(UserTrainings.SUPPORT_PATH, new UserTrainings(true), null);

            putPage(PasswordResetPage.PATH, new PasswordResetPage(), null);

            if (testing) {
                try {
                    Class<?> manager = Class.forName("org.cacert.gigi.pages.Manager");
                    Page p = (Page) manager.getMethod("getInstance").invoke(null);
                    String pa = (String) manager.getField("PATH").get(null);
                    putPage(pa + "/*", p, "Gigi test server");
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }

            try {
                putPage("/wot/rules", new StaticPage("Web of Trust Rules", AssurePage.class.getResourceAsStream("Rules.templ")), "Web of Trust");
            } catch (UnsupportedEncodingException e) {
                throw new ServletException(e);
            }
            baseTemplate = new Template(Gigi.class.getResource("Gigi.templ"));
            rootMenu = new MenuCollector();

            Menu languages = new Menu("Language");
            for (Locale l : Language.getSupportedLocales()) {
                languages.addItem(new SimpleMenuItem("?lang=" + l.toString(), l.getDisplayName(l)));
            }
            categories.add(languages);
            for (Menu menu : categories) {
                menu.prepare();
                rootMenu.put(menu);
            }

            // rootMenu.prepare();
            return rootMenu;
        }

        public Map<String, Page> getPages() {
            return Collections.unmodifiableMap(pages);
        }
    }

    public static final String LOGGEDIN = "loggedin";

    public static final String CERT_SERIAL = "org.cacert.gigi.serial";

    public static final String CERT_ISSUER = "org.cacert.gigi.issuer";

    public static final String AUTH_CONTEXT = "auth";

    public static final String LOGIN_METHOD = "org.cacert.gigi.loginMethod";

    private static final long serialVersionUID = -6386785421902852904L;

    private static Gigi instance;

    private Template baseTemplate;

    private PingerDaemon pinger;

    private KeyStore truststore;

    private boolean testing;

    private MenuCollector rootMenu;

    private Map<String, Page> pages;

    private boolean firstInstanceInited = false;

    public Gigi(Properties conf, KeyStore truststore) {
        synchronized (Gigi.class) {
            if (instance != null) {
                throw new IllegalStateException("Multiple Gigi instances!");
            }
            testing = conf.getProperty("testing") != null;
            instance = this;
            DatabaseConnection.init(conf);
            this.truststore = truststore;
            pinger = new PingerDaemon(truststore);
            pinger.start();
        }
    }

    @Override
    public synchronized void init() throws ServletException {
        if (firstInstanceInited) {
            super.init();
            return;
        }
        // ensure those static initializers are finished
        try (Link l = DatabaseConnection.newLink(false)) {
            CACertificate.getById(1);
            CertificateProfile.getById(1);
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        MenuBuilder mb = new MenuBuilder();
        rootMenu = mb.generateMenu();
        pages = mb.getPages();

        firstInstanceInited = true;
        super.init();
    }

    private Page getPage(String pathInfo) {
        if (pathInfo.endsWith("/") && !pathInfo.equals("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
        }
        Page page = pages.get(pathInfo);
        if (page != null) {
            return page;
        }
        page = pages.get(pathInfo + "/*");
        if (page != null) {
            return page;
        }
        int idx = pathInfo.lastIndexOf('/');
        if (idx == -1 || idx == 0) {
            return null;
        }

        page = pages.get(pathInfo.substring(0, idx) + "/*");
        if (page != null) {
            return page;
        }
        int lIdx = pathInfo.lastIndexOf('/', idx - 1);
        if (lIdx == -1) {
            return null;
        }
        String lastResort = pathInfo.substring(0, lIdx) + "/*" + pathInfo.substring(idx);
        page = pages.get(lastResort);
        return page;

    }

    private static String staticTemplateVarHttp = "http://" + ServerConstants.getStaticHostNamePort();

    private static String staticTemplateVarHttps = "https://" + ServerConstants.getStaticHostNamePortSecure();

    private static String getStaticTemplateVar(boolean https) {
        if (https) {
            return staticTemplateVarHttps;
        } else {
            return staticTemplateVarHttp;
        }
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if ("/error".equals(req.getPathInfo()) || "/denied".equals(req.getPathInfo())) {
            if (DatabaseConnection.hasInstance()) {
                serviceWithConnection(req, resp);
                return;
            }
        }
        try (DatabaseConnection.Link l = DatabaseConnection.newLink( !req.getMethod().equals("POST"))) {
            serviceWithConnection(req, resp);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void serviceWithConnection(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        boolean isSecure = req.isSecure();
        addXSSHeaders(resp, isSecure);
        // Firefox only sends this, if it's a cross domain access; safari sends
        // it always
        String originHeader = req.getHeader("Origin");
        if (originHeader != null //
                &&
                !(originHeader.matches("^" + Pattern.quote("https://" + ServerConstants.getWwwHostNamePortSecure()) + "(/.*|)") || //
                        originHeader.matches("^" + Pattern.quote("http://" + ServerConstants.getWwwHostNamePort()) + "(/.*|)") || //
                originHeader.matches("^" + Pattern.quote("https://" + ServerConstants.getSecureHostNamePort()) + "(/.*|)"))) {
            resp.setContentType("text/html; charset=utf-8");
            resp.getWriter().println("<html><head><title>Alert</title></head><body>No cross domain access allowed.<br/><b>If you don't know why you're seeing this you may have been fished! Please change your password immediately!</b></body></html>");
            return;
        }
        HttpSession hs = req.getSession();
        String clientSerial = (String) hs.getAttribute(CERT_SERIAL);
        if (clientSerial != null) {
            X509Certificate[] cert = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
            if (cert == null || cert[0] == null//
                    || !cert[0].getSerialNumber().toString(16).toUpperCase().equals(clientSerial) //
                    || !cert[0].getIssuerDN().equals(hs.getAttribute(CERT_ISSUER))) {
                hs.invalidate();
                resp.sendError(403, "Certificate mismatch.");
                return;
            }

        }
        if (req.getParameter("lang") != null) {
            Locale l = Language.getLocaleFromString(req.getParameter("lang"));
            Language lu = Language.getInstance(l);
            req.getSession().setAttribute(Language.SESSION_ATTRIB_NAME, lu.getLocale());
        }
        final Page p = getPage(req.getPathInfo());

        if (p != null) {
            if ( !isSecure && (p.needsLogin() || p instanceof LoginPage || p instanceof RegisterPage)) {
                resp.sendRedirect("https://" + ServerConstants.getWwwHostNamePortSecure() + req.getPathInfo());
                return;
            }
            AuthorizationContext currentAuthContext = LoginPage.getAuthorizationContext(req);
            if ( !p.isPermitted(currentAuthContext)) {
                if (hs.getAttribute("loggedin") == null) {
                    String request = req.getPathInfo();
                    request = request.split("\\?")[0];
                    hs.setAttribute(LoginPage.LOGIN_RETURNPATH, request);
                    resp.sendRedirect("/login");
                    return;
                }
                resp.sendError(403);
                return;
            }
            if (p.beforeTemplate(req, resp)) {
                return;
            }
            HashMap<String, Object> vars = new HashMap<String, Object>();
            // System.out.println(req.getMethod() + ": " + req.getPathInfo() +
            // " -> " + p);
            Outputable content = new Outputable() {

                @Override
                public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                    try {
                        if (req.getMethod().equals("POST")) {
                            if (req.getQueryString() != null && !(p instanceof HandlesMixedRequest)) {
                                return;
                            }
                            p.doPost(req, resp);
                        } else {
                            p.doGet(req, resp);
                        }
                    } catch (CSRFException err) {
                        try {
                            resp.sendError(500, "CSRF invalid");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            };
            Language lang = Page.getLanguage(req);

            vars.put(Menu.AUTH_VALUE, currentAuthContext);
            vars.put("menu", rootMenu);
            vars.put("title", lang.getTranslation(p.getTitle()));
            vars.put("static", getStaticTemplateVar(isSecure));
            vars.put("year", Calendar.getInstance().get(Calendar.YEAR));
            vars.put("content", content);
            if (currentAuthContext != null) {
                // TODO maybe move this information into the AuthContext object
                vars.put("loginMethod", req.getSession().getAttribute(LOGIN_METHOD));
                vars.put("authContext", currentAuthContext);

            }
            resp.setContentType("text/html; charset=utf-8");
            baseTemplate.output(resp.getWriter(), lang, vars);
        } else {
            resp.sendError(404, "Page not found.");
        }

    }

    public static void addXSSHeaders(HttpServletResponse hsr, boolean doHttps) {
        hsr.addHeader("Access-Control-Allow-Origin", "https://" + ServerConstants.getWwwHostNamePortSecure() + " https://" + ServerConstants.getSecureHostNamePort());
        hsr.addHeader("Access-Control-Max-Age", "60");
        if (doHttps) {
            hsr.addHeader("Content-Security-Policy", httpsCSP);
        } else {
            hsr.addHeader("Content-Security-Policy", httpCSP);
        }
        hsr.addHeader("Strict-Transport-Security", "max-age=31536000");

    }

    private static String httpsCSP = genHttpsCSP();

    private static String httpCSP = genHttpCSP();

    private static String genHttpsCSP() {
        StringBuffer csp = new StringBuffer();
        csp.append("default-src 'none'");
        csp.append(";font-src https://" + ServerConstants.getStaticHostNamePortSecure());
        csp.append(";img-src https://" + ServerConstants.getStaticHostNamePortSecure());
        csp.append(";media-src 'none'; object-src 'none'");
        csp.append(";script-src https://" + ServerConstants.getStaticHostNamePortSecure());
        csp.append(";style-src https://" + ServerConstants.getStaticHostNamePortSecure());
        csp.append(";form-action https://" + ServerConstants.getSecureHostNamePort() + " https://" + ServerConstants.getWwwHostNamePortSecure());
        // csp.append(";report-url https://api.cacert.org/security/csp/report");
        return csp.toString();
    }

    private static String genHttpCSP() {
        StringBuffer csp = new StringBuffer();
        csp.append("default-src 'none'");
        csp.append(";font-src http://" + ServerConstants.getStaticHostNamePort());
        csp.append(";img-src http://" + ServerConstants.getStaticHostNamePort());
        csp.append(";media-src 'none'; object-src 'none'");
        csp.append(";script-src http://" + ServerConstants.getStaticHostNamePort());
        csp.append(";style-src http://" + ServerConstants.getStaticHostNamePort());
        csp.append(";form-action https://" + ServerConstants.getSecureHostNamePort() + " https://" + ServerConstants.getWwwHostNamePort());
        // csp.append(";report-url http://api.cacert.org/security/csp/report");
        return csp.toString();
    }

    /**
     * Requests Pinging of domains.
     * 
     * @param toReping
     *            if not null, the {@link DomainPingConfiguration} to test, if
     *            null, just re-check if there is something to do.
     */
    public static void notifyPinger(DomainPingConfiguration toReping) {
        if (toReping != null) {
            instance.pinger.queue(toReping);
        }
        instance.pinger.interrupt();
    }

}
