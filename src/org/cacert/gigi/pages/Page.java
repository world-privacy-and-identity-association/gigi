package org.cacert.gigi.pages;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.template.Template;

/**
 * This class encapsulates a sub page of Gigi. A template residing nearby this
 * class with name &lt;className&gt;.templ will be loaded automatically.
 */
public abstract class Page {

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

    public void setTitle(String title) {
        this.title = title;
    }

    public static Language getLanguage(ServletRequest req) {
        return Language.getInstance("de");
    }

    public static String translate(ServletRequest req, String string) {
        Language l = getLanguage(req);
        return l.getTranslation(string);
    }

}
