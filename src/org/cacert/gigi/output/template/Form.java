package org.cacert.gigi.output.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
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

    /**
     * Encapsulates a (non-failure) outcome of a form.
     */
    public static abstract class SubmissionResult {

        public abstract boolean endsForm();
    }

    /**
     * The form has finished and the user should see the successful completion
     * on a regular page.
     */
    public static class RedirectResult extends SubmissionResult {

        private final String target;

        public RedirectResult(String target) {
            this.target = target;
        }

        @Override
        public boolean endsForm() {
            return true;
        }

    }

    /**
     * The form has not finished and should be re-emitted, however no error
     * occurred.
     */
    public static class FormContinue extends SubmissionResult {

        @Override
        public boolean endsForm() {
            return false;
        }
    }

    /**
     * The form has successfully finished and a message should be emitted on a
     * stateful page.
     */
    public static class SuccessMessageResult extends SubmissionResult {

        private final Outputable message;

        public SuccessMessageResult(Outputable message) {
            this.message = message;
        }

        @Override
        public boolean endsForm() {
            return true;
        }
    }

    public static final String CSRF_FIELD = "csrf";

    public static final String SUBMIT_RESULT = "form-submit-result";

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
    public abstract SubmissionResult submit(HttpServletRequest req) throws GigiApiException;

    public boolean submitExceptionProtected(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            SubmissionResult res = submit(req);
            req.setAttribute(SUBMIT_RESULT, res);
            if (res instanceof RedirectResult) {
                resp.sendRedirect(((RedirectResult) res).target);
                return true;
            }
            if (res.endsForm()) {
                HttpSession hs = req.getSession();
                hs.removeAttribute("form/" + getClass().getName() + "/" + csrf);
            }
            return false;
        } catch (PermamentFormException e) {
            req.setAttribute(SUBMIT_RESULT, e);
            return false;
        } catch (GigiApiException e) {
            req.setAttribute(SUBMIT_RESULT, e);
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
     *         reprinted (and it has not already been successfully submitted)
     */
    public static boolean printFormErrors(HttpServletRequest req, PrintWriter out) {
        Object o = req.getAttribute(SUBMIT_RESULT);
        if (o != null && (o instanceof PermamentFormException)) {
            ((PermamentFormException) o).getCause().format(out, Page.getLanguage(req));
            return false;
        }
        if (o != null && (o instanceof GigiApiException)) {
            ((GigiApiException) o).format(out, Page.getLanguage(req));
            return true;
        }
        if (o != null && (o instanceof FormContinue)) {
            return true;
        }
        if (o != null && (o instanceof SuccessMessageResult)) {
            Outputable message = ((SuccessMessageResult) o).message;
            if (message != null) {
                out.println("<div class='alert alert-success'>");
                message.output(out, Page.getLanguage(req), new HashMap<String, Object>());
                out.println("</div>");
            }
            return false;
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
