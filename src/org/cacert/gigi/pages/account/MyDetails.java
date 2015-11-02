package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

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
        MyListingForm listingForm = new MyListingForm(req, getUser(req));
        map.put("detailsForm", form);
        map.put("contactMeForm", listingForm);
        if (LoginPage.getUser(req).getOrganisations().size() != 0) {
            map.put("orgaForm", new MyOrganisationsForm(req));
        }
        getDefaultTemplate().output(out, getLanguage(req), map);
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("orgaForm") != null) {
            Form.getForm(req, MyOrganisationsForm.class).submit(resp.getWriter(), req);
        } else if (req.getParameter("processDetails") != null) {
            MyDetailsForm form = Form.getForm(req, MyDetailsForm.class);
            form.submit(resp.getWriter(), req);
        } else if (req.getParameter("processContact") != null) {
            MyListingForm form = Form.getForm(req, MyListingForm.class);
            form.submit(resp.getWriter(), req);
        } else {
            return false;
        }
        resp.sendRedirect(PATH);
        return true;
    }

}
