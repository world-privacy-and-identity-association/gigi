package club.wpia.gigi;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.DatabaseConnection.Link;
import club.wpia.gigi.dbObjects.CACertificate;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.Menu;
import club.wpia.gigi.output.MenuCollector;
import club.wpia.gigi.output.PageMenuItem;
import club.wpia.gigi.output.SimpleMenuItem;
import club.wpia.gigi.output.SimpleUntranslatedMenuItem;
import club.wpia.gigi.output.template.Form.CSRFException;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.PlainOutputable;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.AboutPage;
import club.wpia.gigi.pages.HandlesMixedRequest;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.LogoutPage;
import club.wpia.gigi.pages.MainPage;
import club.wpia.gigi.pages.OneFormPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.pages.PasswordResetPage;
import club.wpia.gigi.pages.RootCertPage;
import club.wpia.gigi.pages.StaticPage;
import club.wpia.gigi.pages.Verify;
import club.wpia.gigi.pages.account.ChangePasswordPage;
import club.wpia.gigi.pages.account.FindAgentAccess;
import club.wpia.gigi.pages.account.History;
import club.wpia.gigi.pages.account.MyDetails;
import club.wpia.gigi.pages.account.UserTrainings;
import club.wpia.gigi.pages.account.certs.CertificateAdd;
import club.wpia.gigi.pages.account.certs.Certificates;
import club.wpia.gigi.pages.account.domain.DomainOverview;
import club.wpia.gigi.pages.account.domain.EditDomain;
import club.wpia.gigi.pages.account.mail.MailOverview;
import club.wpia.gigi.pages.admin.TTPAdminPage;
import club.wpia.gigi.pages.admin.support.FindCertPage;
import club.wpia.gigi.pages.admin.support.FindUserByDomainPage;
import club.wpia.gigi.pages.admin.support.FindUserByEmailPage;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.pages.admin.support.SupportOrgDomainPage;
import club.wpia.gigi.pages.admin.support.SupportUserDetailsPage;
import club.wpia.gigi.pages.error.AccessDenied;
import club.wpia.gigi.pages.error.PageNotFound;
import club.wpia.gigi.pages.main.CertStatusRequestPage;
import club.wpia.gigi.pages.main.KeyCompromisePage;
import club.wpia.gigi.pages.main.RegisterPage;
import club.wpia.gigi.pages.orga.CreateOrgPage;
import club.wpia.gigi.pages.orga.SwitchOrganisation;
import club.wpia.gigi.pages.orga.ViewOrgPage;
import club.wpia.gigi.pages.statistics.StatisticsRoles;
import club.wpia.gigi.pages.wot.Points;
import club.wpia.gigi.pages.wot.RequestTTPPage;
import club.wpia.gigi.pages.wot.VerifyPage;
import club.wpia.gigi.ping.PingerDaemon;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.DomainAssessment;
import club.wpia.gigi.util.PasswordHash;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;
import club.wpia.gigi.util.TimeConditions;

public final class Gigi extends HttpServlet {

    public static final String LINK_HOST = "linkHost";

    private class MenuBuilder {

        private LinkedList<Menu> categories = new LinkedList<Menu>();

        private HashMap<String, Page> pages = new HashMap<String, Page>();

        private MenuCollector rootMenu;

        public MenuBuilder() {}

        private void putPage(String path, Page p, Menu m) {
            pages.put(path, p);
            if (m == null) {
                return;
            }
            m.addItem(new PageMenuItem(p, path.replaceFirst("/?\\*$", "")));

        }

        private Menu createMenu(String name) {
            Menu m = new Menu(new TranslateCommand(name));
            categories.add(m);
            return m;
        }

        private Menu createMenu(Outputable name) {
            Menu m = new Menu(name);
            categories.add(m);
            return m;
        }

        public MenuCollector generateMenu() throws ServletException {
            putPage("/denied", new AccessDenied(), null);
            putPage("/error", new PageNotFound(), null);
            putPage("/login", new LoginPage(), null);
            Menu mainMenu = createMenu(new PlainOutputable(ServerConstants.getAppName()));
            mainMenu.addItem(new SimpleMenuItem("https://" + ServerConstants.getHostNamePort(Host.WWW) + "/login", "Password Login") {

                @Override
                public boolean isPermitted(AuthorizationContext ac) {
                    return ac == null;
                }
            });
            mainMenu.addItem(new SimpleMenuItem("https://" + ServerConstants.getHostNamePortSecure(Host.SECURE) + "/login", "Certificate Login") {

                @Override
                public boolean isPermitted(AuthorizationContext ac) {
                    return ac == null;
                }
            });
            putPage("/", new MainPage(), null);
            putPage("/roots", new RootCertPage(truststore), mainMenu);
            putPage(StatisticsRoles.PATH, new StatisticsRoles(), mainMenu);
            putPage("/about", new AboutPage(), mainMenu);
            putPage(RegisterPage.PATH, new RegisterPage(), mainMenu);
            putPage(CertStatusRequestPage.PATH, new CertStatusRequestPage(), mainMenu);
            putPage(KeyCompromisePage.PATH, new KeyCompromisePage(), mainMenu);

            putPage(Verify.PATH, new Verify(), null);
            Menu certificates = createMenu("Certificates");
            putPage(Certificates.PATH + "/*", new Certificates(false), certificates);
            putPage(CertificateAdd.PATH, new CertificateAdd(), certificates);

            Menu wot = createMenu("Verification");
            putPage(MailOverview.DEFAULT_PATH, new MailOverview(), wot);
            putPage(DomainOverview.PATH, new DomainOverview(), wot);
            putPage(EditDomain.PATH + "*", new EditDomain(), null);
            putPage(VerifyPage.PATH + "/*", new VerifyPage(), wot);
            putPage(Points.PATH, new Points(false), wot);
            putPage(RequestTTPPage.PATH, new RequestTTPPage(), wot);

            Menu admMenu = createMenu("Admin");
            Menu orgAdm = createMenu("Organisation Admin");
            putPage(TTPAdminPage.PATH + "/*", new TTPAdminPage(), admMenu);
            putPage(CreateOrgPage.DEFAULT_PATH, new CreateOrgPage(), orgAdm);
            putPage(ViewOrgPage.DEFAULT_PATH + "/*", new ViewOrgPage(), orgAdm);
            putPage(SwitchOrganisation.PATH, new SwitchOrganisation(), orgAdm);

            Menu support = createMenu("Support Console");
            putPage(SupportEnterTicketPage.PATH, new SupportEnterTicketPage(), support);
            putPage(FindUserByEmailPage.PATH, new FindUserByEmailPage(), support);
            putPage(FindUserByDomainPage.PATH, new FindUserByDomainPage(), support);
            putPage(FindCertPage.PATH, new FindCertPage(), support);

            Menu account = createMenu("My Account");
            putPage(SupportUserDetailsPage.PATH + "*", new SupportUserDetailsPage(), null);
            putPage(SupportOrgDomainPage.PATH + "*", new SupportOrgDomainPage(), null);
            putPage(ChangePasswordPage.PATH, new ChangePasswordPage(), account);
            putPage(History.PATH, new History(false), account);
            putPage(FindAgentAccess.PATH, new OneFormPage("Access to Find Agent", FindAgentAccess.class), account);
            putPage(History.SUPPORT_PATH, new History(true), null);
            putPage(UserTrainings.PATH, new UserTrainings(false), account);
            putPage(MyDetails.PATH, new MyDetails(), account);
            putPage(UserTrainings.SUPPORT_PATH, new UserTrainings(true), null);
            putPage(Points.SUPPORT_PATH, new Points(true), null);
            putPage(Certificates.SUPPORT_PATH + "/*", new Certificates(true), null);

            putPage(PasswordResetPage.PATH, new PasswordResetPage(), null);
            putPage(LogoutPage.PATH, new LogoutPage(), null);

            if (testing) {
                try {
                    Class<?> manager = Class.forName("club.wpia.gigi.pages.Manager");
                    Page p = (Page) manager.getMethod("getInstance").invoke(null);
                    String pa = (String) manager.getField("PATH").get(null);
                    Menu testServer = createMenu("Gigi test server");
                    putPage(pa + "/*", p, testServer);
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }

            try {
                putPage("/wot/rules", new StaticPage("Verification Rules", VerifyPage.class.getResourceAsStream("Rules.templ")), wot);
            } catch (UnsupportedEncodingException e) {
                throw new ServletException(e);
            }
            rootMenu = new MenuCollector();

            Menu languages = createMenu("Language");
            addLanguages(languages);
            for (Menu menu : categories) {
                menu.prepare();
                rootMenu.put(menu);
            }

            // rootMenu.prepare();
            return rootMenu;
        }

        private void addLanguages(Menu languages) {
            for (Locale l : Language.getSupportedLocales()) {
                languages.addItem(new SimpleUntranslatedMenuItem("?lang=" + l.toString(), l.getDisplayName(l)));
            }
        }

        public Map<String, Page> getPages() {
            return Collections.unmodifiableMap(pages);
        }
    }

    public static final String LOGGEDIN = "loggedin";

    public static final String CERT_SERIAL = "club.wpia.gigi.serial";

    public static final String CERT_ISSUER = "club.wpia.gigi.issuer";

    public static final String AUTH_CONTEXT = "auth";

    public static final String LOGIN_METHOD = "club.wpia.gigi.loginMethod";

    private static final long serialVersionUID = -6386785421902852904L;

    private static Gigi instance;

    private static final Template baseTemplate = new Template(Gigi.class.getResource("Gigi.templ"));

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
            DomainAssessment.init(conf);
            DatabaseConnection.init(conf);
            TimeConditions.init(conf);
            PasswordHash.init(conf);
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
            CATSType.AGENT_CHALLENGE.getDisplayName();
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

    private static String staticTemplateVar = "//" + ServerConstants.getHostNamePort(Host.STATIC);

    private static String staticTemplateVarSecure = "//" + ServerConstants.getHostNamePortSecure(Host.STATIC);

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
                && !(originHeader.matches("^" + Pattern.quote("https://" + ServerConstants.getHostNamePortSecure(Host.WWW)) + "(/.*|)") || //
                        originHeader.matches("^" + Pattern.quote("http://" + ServerConstants.getHostNamePort(Host.WWW)) + "(/.*|)") || //
                        originHeader.matches("^" + Pattern.quote("https://" + ServerConstants.getHostNamePortSecure(Host.SECURE)) + "(/.*|)"))) {
            resp.setContentType("text/html; charset=utf-8");
            resp.getWriter().println("<html><head><title>Alert</title></head><body>No cross domain access allowed.<br/><b>If you don't know why you're seeing this you may have been fished! Please change your password immediately!</b></body></html>");
            return;
        }
        HttpSession hs = req.getSession();
        BigInteger clientSerial = (BigInteger) hs.getAttribute(CERT_SERIAL);
        if (clientSerial != null) {
            X509Certificate[] cert = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
            if (cert == null || cert[0] == null//
                    || !cert[0].getSerialNumber().equals(clientSerial) //
                    || !cert[0].getIssuerDN().equals(hs.getAttribute(CERT_ISSUER))) {
                hs.invalidate();
                resp.sendError(403, "Certificate mismatch.");
                return;
            }

        }
        if (req.getParameter("lang") != null) {
            Locale l = Language.getLocaleFromString(req.getParameter("lang"));
            Language lu = Language.getInstance(l);
            req.getSession().setAttribute(Language.SESSION_ATTRIB_NAME, lu != null ? lu.getLocale() : Locale.ENGLISH);
        }
        final Page p = getPage(req.getPathInfo());

        if (p != null) {
            if ( !isSecure && (p.needsLogin() || p instanceof LoginPage || p instanceof RegisterPage)) {
                resp.sendRedirect("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + req.getPathInfo());
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
            try {
                if (p.beforeTemplate(req, resp)) {
                    return;
                }
            } catch (CSRFException e) {
                resp.sendError(500, "CSRF invalid");
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
            vars.put("static", isSecure ? staticTemplateVarSecure : staticTemplateVar);
            vars.put("year", Calendar.getInstance().get(Calendar.YEAR));
            vars.put("content", content);
            if (isSecure) {
                req.setAttribute(LINK_HOST, ServerConstants.getHostNamePortSecure(Host.LINK));
            } else {
                req.setAttribute(LINK_HOST, ServerConstants.getHostNamePort(Host.LINK));
            }
            vars.put(Gigi.LINK_HOST, req.getAttribute(Gigi.LINK_HOST));
            if (currentAuthContext != null) {
                // TODO maybe move this information into the AuthContext object
                vars.put("loginMethod", req.getSession().getAttribute(LOGIN_METHOD));
                vars.put("authContext", currentAuthContext);

            }
            vars.put("appName", ServerConstants.getAppName());
            resp.setContentType("text/html; charset=utf-8");
            baseTemplate.output(resp.getWriter(), lang, vars);
        } else {
            resp.sendError(404, "Page not found.");
        }

    }

    public static void addXSSHeaders(HttpServletResponse hsr, boolean doHttps) {
        hsr.addHeader("Access-Control-Allow-Origin", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + " https://" + ServerConstants.getHostNamePortSecure(Host.SECURE));
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
        csp.append(";font-src https://" + ServerConstants.getHostNamePortSecure(Host.STATIC));
        csp.append(";img-src https://" + ServerConstants.getHostNamePortSecure(Host.STATIC));
        csp.append(";media-src 'none'; object-src 'none'");
        csp.append(";script-src https://" + ServerConstants.getHostNamePortSecure(Host.STATIC));
        csp.append(";style-src https://" + ServerConstants.getHostNamePortSecure(Host.STATIC));
        csp.append(";form-action https://" + ServerConstants.getHostNamePortSecure(Host.SECURE) + " https://" + ServerConstants.getHostNamePortSecure(Host.WWW));
        // csp.append(";report-url https://api.wpia.club/security/csp/report");
        return csp.toString();
    }

    private static String genHttpCSP() {
        StringBuffer csp = new StringBuffer();
        csp.append("default-src 'none'");
        csp.append(";font-src http://" + ServerConstants.getHostNamePort(Host.STATIC));
        csp.append(";img-src http://" + ServerConstants.getHostNamePort(Host.STATIC));
        csp.append(";media-src 'none'; object-src 'none'");
        csp.append(";script-src http://" + ServerConstants.getHostNamePort(Host.STATIC));
        csp.append(";style-src http://" + ServerConstants.getHostNamePort(Host.STATIC));
        csp.append(";form-action http://" + ServerConstants.getHostNamePortSecure(Host.SECURE) + " http://" + ServerConstants.getHostNamePort(Host.WWW));
        // csp.append(";report-url http://api.wpia.club/security/csp/report");
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
