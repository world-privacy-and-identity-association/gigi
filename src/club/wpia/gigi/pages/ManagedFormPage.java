package club.wpia.gigi.pages;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Form;

public abstract class ManagedFormPage extends Page {

    Class<? extends Form> c;

    public ManagedFormPage(String title, Class<? extends Form> t) {
        super(title);
        c = t;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form form = Form.getForm(req, c);
            form.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        }
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, c).submitExceptionProtected(req, resp);
    }

}
