package club.wpia.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Form.CSRFException;

public abstract class ManagedMultiFormPage extends Page {

    public ManagedMultiFormPage(String title) {
        super(title);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            getForm(req).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return getForm(req).submitExceptionProtected(req, resp);
    }

    public abstract Form getForm(HttpServletRequest req) throws CSRFException;

}
