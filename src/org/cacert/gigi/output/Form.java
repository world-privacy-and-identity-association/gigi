package org.cacert.gigi.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

public abstract class Form implements Outputable {

    public static final String CSRF_FIELD = "csrf";

    private String csrf;

    public Form(HttpServletRequest hsr) {
        csrf = RandomToken.generateToken(32);
        HttpSession hs = hsr.getSession();
        hs.setAttribute("form/" + getClass().getName() + "/" + csrf, this);

    }

    public abstract boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException;

    protected String getCsrfFieldName() {
        return CSRF_FIELD;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<form method='POST'>");
        failed = false;
        outputContent(out, l, vars);
        out.print("<input type='hidden' name='" + CSRF_FIELD + "' value='");
        out.print(getCSRFToken());
        out.println("'></form>");
    }

    protected abstract void outputContent(PrintWriter out, Language l, Map<String, Object> vars);

    boolean failed;

    protected void outputError(PrintWriter out, ServletRequest req, String text, Object... contents) {
        if ( !failed) {
            failed = true;
            out.println("<div class='formError'>");
        }
        out.print("<div>");
        if (contents.length == 0) {
            out.print(Page.translate(req, text));
        } else {
            out.print(String.format(Page.translate(req, text), contents));
        }
        out.println("</div>");
    }

    protected void outputErrorPlain(PrintWriter out, String text) {
        if ( !failed) {
            failed = true;
            out.println("<div class='formError'>");
        }
        out.print("<div>");
        out.print(text);
        out.println("</div>");
    }

    public boolean isFailed(PrintWriter out) {
        if (failed) {
            out.println("</div>");
        }
        return failed;
    }

    protected String getCSRFToken() {
        return csrf;
    }

    public static <T extends Form> T getForm(HttpServletRequest req, Class<T> target) throws CSRFException {
        String csrf = req.getParameter(CSRF_FIELD);
        if (csrf == null) {
            throw new CSRFException();
        }
        HttpSession hs = req.getSession();
        if (hs == null) {
            throw new CSRFException();
        }
        Form f = (Form) hs.getAttribute("form/" + target.getName() + "/" + csrf);
        if (f == null) {
            throw new CSRFException();
        }
        return (T) f;
    }

    public static class CSRFException extends IOException {

    }
}
