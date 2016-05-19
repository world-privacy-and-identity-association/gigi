package org.cacert.gigi.pages;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.output.template.Form;

public abstract class OneFormPage extends Page {

    Class<? extends Form> c;

    public OneFormPage(String title, Class<? extends Form> t) {
        super(title);
        c = t;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Form form = Form.getForm(req, c);
            if (form.submit(resp.getWriter(), req)) {
                resp.sendRedirect(getSuccessPath(form));
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
            doGet(req, resp);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            c.getConstructor(HttpServletRequest.class).newInstance(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } catch (ReflectiveOperationException e) {
            new GigiApiException().format(resp.getWriter(), getLanguage(req));
        }
    }

    public abstract String getSuccessPath(Form f);

}
