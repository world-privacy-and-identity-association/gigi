package org.cacert.gigi.output.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

/**
 * A generic HTML-form that handles CSRF-token creation.
 */
public abstract class Form implements Outputable {

    public static class PermamentFormException extends RuntimeException {

        public PermamentFormException(GigiApiException cause) {
            super(cause);
        }

        @Override
        public synchronized GigiApiException getCause() {
            return (GigiApiException) super.getCause();
        }
    }

    public static final String CSRF_FIELD = "csrf";

    private static final String SUBMIT_EXCEPTION = "form-submit-exception";

    private final String csrf;

    private final String action;

    /**
     * Creates a new {@link Form}.
     * 
     * @param hsr
     *            the request to register the form against.
     */
    public Form(HttpServletRequest hsr) {
        this(hsr, null);
    }

    /**
     * Creates a new {@link Form}.
     * 
     * @param hsr
     *            the request to register the form against.
     * @param action
     *            the target path where the form should be submitted.
     */
    public Form(HttpServletRequest hsr, String action) {
        csrf = RandomToken.generateToken(32);
        this.action = action;
        HttpSession hs = hsr.getSession();
        hs.setAttribute("form/" + getClass().getName() + "/" + csrf, this);
    }

    /**
     * Update the forms internal state based on submitted data.
     * 
     * @param req
     *            the request to take the initial data from.
     * @return true, iff the form succeeded and the user should be redirected.
     * @throws GigiApiException
     *             if form data had problems or operations went wrong.
     */
    public abstract boolean submit(HttpServletRequest req) throws GigiApiException;

    /**
     * Calls {@link #submit(PrintWriter, HttpServletRequest)} while catching and
     * displaying errors ({@link GigiApiException}), and re-outputing the form
     * via {@link #output(PrintWriter, Language, Map)}.
     * 
     * @param out
     *            the target to write the form and errors to
     * @param req
     *            the request that this submit originated (for submit and for
     *            language)
     * @return as {@link #submit(PrintWriter, HttpServletRequest)}: true, iff
     *         the form succeeded and the user should be redirected.
     */
    public boolean submitProtected(PrintWriter out, HttpServletRequest req) {
        try {
            boolean succeeded = submit(req);
            if (succeeded) {
                HttpSession hs = req.getSession();
                hs.removeAttribute("form/" + getClass().getName() + "/" + csrf);
                return true;
            }
        } catch (GigiApiException e) {
            e.format(out, LoginPage.getLanguage(req));
        }
        output(out, LoginPage.getLanguage(req), new HashMap<String, Object>());
        return false;
    }

    public boolean submitExceptionProtected(HttpServletRequest req) {
        try {
            if (submit(req)) {
                HttpSession hs = req.getSession();
                hs.removeAttribute("form/" + getClass().getName() + "/" + csrf);
                return true;
            }
            return false;
        } catch (PermamentFormException e) {
            req.setAttribute(SUBMIT_EXCEPTION, e);
            return false;
        } catch (GigiApiException e) {
            req.setAttribute(SUBMIT_EXCEPTION, e);
            return false;
        }
    }

    /**
     * Prints any errors in any form submits on this request.
     * 
     * @param req
     *            The request to extract the errors from.
     * @param out
     *            the output stream to the user to write the errors to.
     * @return true if no permanent errors occurred and the form should be
     *         reprinted.
     */
    public static boolean printFormErrors(HttpServletRequest req, PrintWriter out) {
        Object o = req.getAttribute(SUBMIT_EXCEPTION);
        if (o != null && (o instanceof PermamentFormException)) {
            ((PermamentFormException) o).getCause().format(out, Page.getLanguage(req));
            return false;
        }
        if (o != null && (o instanceof GigiApiException)) {
            ((GigiApiException) o).format(out, Page.getLanguage(req));
        }
        return true;
    }

    protected String getCsrfFieldName() {
        return CSRF_FIELD;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (action == null) {
            out.println("<form method='POST'>");
        } else {
            out.println("<form method='POST' action='" + action + "'>");
        }
        outputContent(out, l, vars);
        out.print("<input type='hidden' name='" + CSRF_FIELD + "' value='");
        out.print(getCSRFToken());
        out.println("'></form>");
    }

    /**
     * Outputs the forms contents.
     * 
     * @param out
     *            Stream to the user.
     * @param l
     *            {@link Language} to translate text to.
     * @param vars
     *            Variables supplied from the outside.
     */
    protected abstract void outputContent(PrintWriter out, Language l, Map<String, Object> vars);

    protected String getCSRFToken() {
        return csrf;
    }

    /**
     * Re-fetches a form e.g. when a Post-request is received.
     * 
     * @param req
     *            the request that is directed to the form.
     * @param target
     *            the {@link Class} of the expected form.
     * @return the form where this request is directed to.
     * @throws CSRFException
     *             if no CSRF-token is found or the token is wrong.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Form> T getForm(HttpServletRequest req, Class<T> target) throws CSRFException {
        String csrf = req.getParameter(CSRF_FIELD);
        if (csrf == null) {
            throw new CSRFException();
        }
        HttpSession hs = req.getSession();
        if (hs == null) {
            throw new CSRFException();
        }
        Object f = hs.getAttribute("form/" + target.getName() + "/" + csrf);
        if (f == null) {
            throw new CSRFException();
        }
        if ( !(f instanceof Form)) {
            throw new CSRFException();
        }
        if ( !target.isInstance(f)) {
            throw new CSRFException();
        }
        // Dynamic Cast checked by previous if statement
        return (T) f;
    }

    public static class CSRFException extends IOException {

        private static final long serialVersionUID = 59708247477988362L;

    }
}
