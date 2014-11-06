package org.cacert.gigi;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.Calendar;
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
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form.CSRFException;
import org.cacert.gigi.output.Menu;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.output.PageMenuItem;
import org.cacert.gigi.output.SimpleMenuItem;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.LogoutPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PolicyIndex;
import org.cacert.gigi.pages.RootCertPage;
import org.cacert.gigi.pages.StaticPage;
import org.cacert.gigi.pages.TestSecure;
import org.cacert.gigi.pages.Verify;
import org.cacert.gigi.pages.account.ChangePasswordPage;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.pages.account.certs.CertificateAdd;
import org.cacert.gigi.pages.account.certs.Certificates;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.pages.account.mail.MailOverview;
import org.cacert.gigi.pages.admin.TTPAdminPage;
import org.cacert.gigi.pages.error.PageNotFound;
import org.cacert.gigi.pages.main.RegisterPage;
import org.cacert.gigi.pages.orga.CreateOrgPage;
import org.cacert.gigi.pages.orga.ViewOrgPage;
import org.cacert.gigi.pages.wot.AssurePage;
import org.cacert.gigi.pages.wot.MyPoints;
import org.cacert.gigi.pages.wot.RequestTTPPage;
import org.cacert.gigi.ping.PingerDaemon;
import org.cacert.gigi.util.ServerConstants;

public class Gigi extends HttpServlet {

    private boolean firstInstanceInited = false;

    public static final String LOGGEDIN = "loggedin";

    public static final String USER = "user";

    private static final long serialVersionUID = -6386785421902852904L;

    private Template baseTemplate;

    private LinkedList<Menu> categories = new LinkedList<Menu>();

    private HashMap<String, Page> pages = new HashMap<String, Page>();

    private HashMap<Page, String> reveresePages = new HashMap<Page, String>();

    private Menu rootMenu;

    private static Gigi instance;

    private PingerDaemon pinger;

    private KeyStore truststore;

    private boolean testing;

    public Gigi(Properties conf, KeyStore truststore) {
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

    @Override
    public void init() throws ServletException {
        if ( !firstInstanceInited) {
            putPage("/error", new PageNotFound(), null);
            putPage("/login", new LoginPage("CAcert - Login"), "CAcert.org");
            putPage("/", new MainPage("CAcert - Home"), null);
            putPage("/roots", new RootCertPage(truststore), "CAcert.org");
            putPage(ChangePasswordPage.PATH, new ChangePasswordPage(), "My Account");
            putPage(LogoutPage.PATH, new LogoutPage("Logout"), "My Account");
            putPage("/secure", new TestSecure(), null);
            putPage(Verify.PATH, new Verify(), null);
            putPage(AssurePage.PATH + "/*", new AssurePage(), "CAcert Web of Trust");
            putPage(Certificates.PATH + "/*", new Certificates(), "Certificates");
            putPage(MyDetails.PATH, new MyDetails(), "My Account");
            putPage(RegisterPage.PATH, new RegisterPage(), "CAcert.org");
            putPage(CertificateAdd.PATH, new CertificateAdd(), "Certificates");
            putPage(MailOverview.DEFAULT_PATH, new MailOverview("My email addresses"), "Certificates");
            putPage(DomainOverview.PATH + "*", new DomainOverview("Domains"), "Certificates");
            putPage(MyPoints.PATH, new MyPoints("My Points"), "CAcert Web of Trust");
            putPage(RequestTTPPage.PATH, new RequestTTPPage(), "CAcert Web of Trust");
            putPage(TTPAdminPage.PATH + "/*", new TTPAdminPage(), "Admin");
            putPage(CreateOrgPage.DEFAULT_PATH, new CreateOrgPage(), "Organisation Admin");
            putPage(ViewOrgPage.DEFAULT_PATH + "/*", new ViewOrgPage(), "Organisation Admin");
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

            putPage("/wot/rules", new StaticPage("CAcert Web of Trust Rules", AssurePage.class.getResourceAsStream("Rules.templ")), "CAcert Web of Trust");
            baseTemplate = new Template(Gigi.class.getResource("Gigi.templ"));
            rootMenu = new Menu("Main");
            Menu about = new Menu("About CAcert.org");
            categories.add(about);

            about.addItem(new SimpleMenuItem("//blog.cacert.org/", "CAcert News"));
            about.addItem(new SimpleMenuItem("//wiki.cacert.org/", "Wiki Documentation"));
            putPage(PolicyIndex.DEFAULT_PATH, new PolicyIndex(), "About CAcert.org");
            about.addItem(new SimpleMenuItem("//wiki.cacert.org/FAQ/Privileges", "Point System"));
            about.addItem(new SimpleMenuItem("//bugs.cacert.org/", "Bug Database"));
            about.addItem(new SimpleMenuItem("//wiki.cacert.org/Board", "CAcert Board"));
            about.addItem(new SimpleMenuItem("//lists.cacert.org/wws", "Mailing Lists"));
            about.addItem(new SimpleMenuItem("//blog.CAcert.org/feed", "RSS News Feed"));

            Menu languages = new Menu("Translations");
            for (Locale l : Language.getSupportedLocales()) {
                languages.addItem(new SimpleMenuItem("?lang=" + l.toString(), l.getDisplayName(l)));
            }
            categories.add(languages);
            for (Menu menu : categories) {
                menu.prepare();
                rootMenu.addItem(menu);
            }

            rootMenu.prepare();
            firstInstanceInited = true;
        }
        super.init();
    }

    private void putPage(String path, Page p, String category) {
        pages.put(path, p);
        reveresePages.put(p, path);
        if (category == null) {
            return;
        }
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
        m.addItem(new PageMenuItem(p));

    }

    private static String staticTemplateVarHttp;

    private static String staticTemplateVarHttps;

    private static String getStaticTemplateVar(boolean https) {
        if (https) {
            if (staticTemplateVarHttps == null) {
                staticTemplateVarHttps = "https://" + ServerConstants.getStaticHostNamePortSecure();
            }
            return staticTemplateVarHttps;
        } else {
            if (staticTemplateVarHttp == null) {
                staticTemplateVarHttp = "http://" + ServerConstants.getStaticHostNamePort();
            }
            return staticTemplateVarHttp;
        }
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        boolean isSecure = req.getServerPort() == ServerConstants.getSecurePort();
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
            User currentPageUser = LoginPage.getUser(req);
            if ( !p.isPermitted(currentPageUser)) {
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
            Outputable content = new Outputable() {

                @Override
                public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                    try {
                        if (req.getMethod().equals("POST")) {
                            if (req.getQueryString() != null) {
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
            vars.put(Menu.USER_VALUE, currentPageUser);
            vars.put("menu", rootMenu);
            vars.put("title", Page.getLanguage(req).getTranslation(p.getTitle()));
            vars.put("static", getStaticTemplateVar(isSecure));
            vars.put("year", Calendar.getInstance().get(Calendar.YEAR));
            vars.put("content", content);
            resp.setContentType("text/html; charset=utf-8");
            baseTemplate.output(resp.getWriter(), Page.getLanguage(req), vars);
        } else {
            resp.sendError(404, "Page not found.");
        }

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
        pathInfo = pathInfo.substring(0, idx);

        page = pages.get(pathInfo + "/*");
        if (page != null) {
            return page;
        }
        return null;

    }

    public static void addXSSHeaders(HttpServletResponse hsr, boolean doHttps) {
        hsr.addHeader("Access-Control-Allow-Origin", "https://" + ServerConstants.getWwwHostNamePortSecure() + " https://" + ServerConstants.getSecureHostNamePort());
        hsr.addHeader("Access-Control-Max-Age", "60");
        if (doHttps) {
            hsr.addHeader("Content-Security-Policy", getHttpsCSP());
        } else {
            hsr.addHeader("Content-Security-Policy", getHttpCSP());
        }
        hsr.addHeader("Strict-Transport-Security", "max-age=31536000");

    }

    private static String httpsCSP = null;

    private static String httpCSP = null;

    private static String getHttpsCSP() {
        if (httpsCSP == null) {
            StringBuffer csp = new StringBuffer();
            csp.append("default-src 'none'");
            csp.append(";font-src https://" + ServerConstants.getStaticHostNamePortSecure());
            csp.append(";img-src https://" + ServerConstants.getStaticHostNamePortSecure());
            csp.append(";media-src 'none'; object-src 'none'");
            csp.append(";script-src https://" + ServerConstants.getStaticHostNamePortSecure());
            csp.append(";style-src https://" + ServerConstants.getStaticHostNamePortSecure());
            csp.append(";form-action https://" + ServerConstants.getSecureHostNamePort() + " https://" + ServerConstants.getWwwHostNamePortSecure());
            csp.append(";report-url https://api.cacert.org/security/csp/report");
            httpsCSP = csp.toString();
        }
        return httpsCSP;
    }

    private static String getHttpCSP() {
        if (httpCSP == null) {
            StringBuffer csp = new StringBuffer();
            csp.append("default-src 'none'");
            csp.append(";font-src http://" + ServerConstants.getStaticHostNamePort());
            csp.append(";img-src http://" + ServerConstants.getStaticHostNamePort());
            csp.append(";media-src 'none'; object-src 'none'");
            csp.append(";script-src http://" + ServerConstants.getStaticHostNamePort());
            csp.append(";style-src http://" + ServerConstants.getStaticHostNamePort());
            csp.append(";form-action https://" + ServerConstants.getSecureHostNamePort() + " https://" + ServerConstants.getWwwHostNamePort());
            csp.append(";report-url http://api.cacert.org/security/csp/report");
            httpCSP = csp.toString();
        }
        return httpCSP;
    }

    public static String getPathByPage(Page p) {
        return instance.reveresePages.get(p).replaceFirst("/?\\*$", "");
    }

    public static void notifyPinger() {
        instance.pinger.interrupt();
    }

}
