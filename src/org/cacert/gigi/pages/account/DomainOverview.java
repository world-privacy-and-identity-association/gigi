package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.User;
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
        DomainAddForm domAdd = new DomainAddForm(req);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("doms", u.getDomains());
        vars.put("domainman", domMan);
        vars.put("domainadd", domAdd);
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

}
