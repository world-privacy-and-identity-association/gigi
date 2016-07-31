package org.cacert.gigi.output.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.RandomToken;

/**
 * A generic HTML-form that handles CSRF-token creation.
 */
public abstract class Form implements Outputable {

    public static final String CSRF_FIELD = "csrf";

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
     * @param out
     *            the stream to the user.
     * @param req
     *            the request to take the initial data from.
     * @return true, iff the form succeeded and the user should be redirected.
     * @throws GigiApiException
     *             if internal operations went wrong.
     */
    public abstract boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException;

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
