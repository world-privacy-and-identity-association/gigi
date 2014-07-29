package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class DomainOverview extends Page {

    public static final String PATH = "/account/domains";

    public DomainOverview(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = getUser(req);
        DomainManagementForm domMan = new DomainManagementForm(req);
        DomainAddForm domAdd = new DomainAddForm(req, u);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("doms", u.getDomains());
        vars.put("domainman", domMan);
        vars.put("domainadd", domAdd);
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("adddomain") != null) {
            DomainAddForm f = Form.getForm(req, DomainAddForm.class);
            f.submit(resp.getWriter(), req);
        } else if (req.getParameter("") != null) {
            DomainManagementForm f = Form.getForm(req, DomainManagementForm.class);
            f.submit(resp.getWriter(), req);
        }
        super.doPost(req, resp);
    }
}
