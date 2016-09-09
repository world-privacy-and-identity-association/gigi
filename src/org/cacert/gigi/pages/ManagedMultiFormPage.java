package org.cacert.gigi.pages;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Form.CSRFException;

public abstract class ManagedMultiFormPage extends Page {

    public ManagedMultiFormPage(String title) {
        super(title);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            getForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        }
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return getForm(req).submitExceptionProtected(req, resp);
    }

    public abstract Form getForm(HttpServletRequest req) throws CSRFException;

}
