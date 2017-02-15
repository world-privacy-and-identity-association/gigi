package club.wpia.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;

public class MyDetails extends Page {

    public MyDetails() {
        super("My Details");
    }

    public static final String PATH = "/account/details";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        HashMap<String, Object> map = new HashMap<String, Object>();
        MyDetailsForm form = new MyDetailsForm(req, getUser(req));
        map.put("detailsForm", form);
        if (LoginPage.getUser(req).getOrganisations().size() != 0) {
            map.put("orgaForm", new MyOrganisationsForm(req));
        }
        getDefaultTemplate().output(out, getLanguage(req), map);
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("orgaForm") != null) {
            return Form.getForm(req, MyOrganisationsForm.class).submitExceptionProtected(req, resp);
        }
        if (req.getParameter("action") != null || req.getParameter("removeName") != null || req.getParameter("deprecateName") != null || req.getParameter("preferred") != null) {
            return Form.getForm(req, MyDetailsForm.class).submitExceptionProtected(req, resp);
        }
        return false;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            if (req.getParameter("orgaForm") != null) {
                Form.getForm(req, MyOrganisationsForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            }
            if (req.getParameter("action") != null || req.getParameter("removeName") != null || req.getParameter("deprecateName") != null || req.getParameter("preferred") != null) {
                Form.getForm(req, MyDetailsForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            }
        }
    }
}
