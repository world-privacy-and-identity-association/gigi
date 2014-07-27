package org.cacert.gigi.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.Language;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

public abstract class Form implements Outputable {

    public static final String CSRF_FIELD = "csrf";

    String csrf;

    public Form(HttpServletRequest hsr) {
        csrf = RandomToken.generateToken(32);
        HttpSession hs = hsr.getSession();
        hs.setAttribute("form/" + getClass().getName() + "/" + csrf, this);

    }

    public abstract boolean submit(PrintWriter out, HttpServletRequest req);

    protected String getCsrfFieldName() {
        return CSRF_FIELD;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<form method='POST' autocomplete='off'>");
        outputContent(out, l, vars);
        out.print("<input type='hidden' name='" + CSRF_FIELD + "' value='");
        out.print(getCSRFToken());
        out.println("'></form>");
    }

    protected abstract void outputContent(PrintWriter out, Language l, Map<String, Object> vars);

    protected void outputError(PrintWriter out, ServletRequest req, String text) {
        out.print("<div>");
        out.print(Page.translate(req, text));
        out.println("</div>");
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
