package club.wpia.gigi.pages;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.PermissionCheckable;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.ServerConstants;

/**
 * This class encapsulates a sub page of Gigi. A template residing nearby this
 * class with name &lt;className&gt;.templ will be loaded automatically.
 */
public abstract class Page implements PermissionCheckable {

    private String title;

    private Template defaultTemplate;

    public Page(String title) {
        this.title = title;
        URL resource = getClass().getResource(getClass().getSimpleName() + ".templ");
        if (resource != null) {
            defaultTemplate = new Template(resource);
        }
    }

    /**
     * Retrieves the default template (&lt;className&gt;.templ) which has
     * already been loaded.
     * 
     * @return the default template.
     */
    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    /**
     * This method can be overridden to execute code and do stuff before the
     * default template is applied.
     * 
     * @param req
     *            the request to handle.
     * @param resp
     *            the response to write to
     * @return true, if the request is consumed and the default template should
     *         not be applied.
     * @throws IOException
     *             if output goes wrong.
     */
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getMethod().equals("POST")) {
            return beforePost(req, resp);
        }
        return false;
    }

    /**
     * This method can be overridden to execute code and do stuff before the
     * default template is applied when the request is a post request and the
     * default implementation of
     * {@link #beforeTemplate(HttpServletRequest, HttpServletResponse)} is
     * called.
     * 
     * @param req
     *            the request to handle.
     * @param resp
     *            the response to write to
     * @return true, if the request is consumed and the default template should
     *         not be applied.
     * @throws IOException
     *             if output goes wrong.
     */
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return false;
    }

    /**
     * This method is called to generate the content inside the default
     * template.
     * 
     * @param req
     *            the request to handle.
     * @param resp
     *            the response to write to
     * @throws IOException
     *             if output goes wrong.
     */
    public abstract void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException;

    /**
     * Same as {@link #doGet(HttpServletRequest, HttpServletResponse)} but for
     * POST requests. By default they are redirected to
     * {@link #doGet(HttpServletRequest, HttpServletResponse)};
     * 
     * @param req
     *            the request to handle.
     * @param resp
     *            the response to write to
     * @throws IOException
     *             if output goes wrong.
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    /**
     * Returns true, if this page requires login. Default is <code>true</code>
     * 
     * @return if the page needs login.
     */
    public boolean needsLogin() {
        return true;
    }

    public String getTitle() {
        return title;
    }

    public static Language getLanguage(ServletRequest req) {
        HttpSession session = ((HttpServletRequest) req).getSession();
        synchronized (session) {

            Locale sessval = (Locale) session.getAttribute(Language.SESSION_ATTRIB_NAME);
            if (sessval != null) {
                Language l = Language.getInstance(sessval);
                if (l != null) {
                    return l;
                }
            }
            Enumeration<Locale> langs = req.getLocales();
            while (langs.hasMoreElements()) {
                Locale c = langs.nextElement();
                Language l = Language.getInstance(c);
                if (l != null) {
                    session.setAttribute(Language.SESSION_ATTRIB_NAME, l.getLocale());
                    return l;
                }
            }
            session.setAttribute(Language.SESSION_ATTRIB_NAME, Locale.ENGLISH);
            return Language.getInstance(Locale.ENGLISH);
        }
    }

    public static String translate(ServletRequest req, String string) {
        Language l = getLanguage(req);
        return l.getTranslation(string);
    }

    public static User getUser(HttpServletRequest req) {
        return LoginPage.getUser(req);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return !needsLogin() || ac != null;
    }

    /**
     * Derive a default variable map for template processing.
     * 
     * @param req
     *            the HTTP-request to derive the map from
     * @return the map
     */
    public static Map<String, Object> getDefaultVars(HttpServletRequest req) {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(Gigi.LINK_HOST, req.getAttribute(Gigi.LINK_HOST));
        vars.put("appName", ServerConstants.getAppName());
        return vars;
    }

}
