package org.cacert.gigi.pages.account.domain;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class DomainOverview extends Page {

    public static final String PATH = "/account/domains/";

    public DomainOverview(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = getUser(req);
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
            int i = Integer.parseInt(pi.substring(PATH.length()));
            Domain d = Domain.getById(i);
            if (u.getId() != d.getOwner().getId()) {
                System.out.println(u.getId());
                System.out.println(d.getOwner().getId());
                return;
            }
            new DomainPinglogForm(req, d).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            try {
                new PingconfigForm(req, d).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), getLanguage(req));
            }
            return;

        }
        try {
            DomainManagementForm domMan = new DomainManagementForm(req, u);
            DomainAddForm domAdd = new DomainAddForm(req, u);
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("doms", u.getDomains());
            vars.put("domainman", domMan);
            vars.put("domainadd", domAdd);
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = getUser(req);
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
            int i = Integer.parseInt(pi.substring(PATH.length()));
            Domain d = Domain.getById(i);
            if (u.getId() != d.getOwner().getId()) {
                return;
            }
            int reping = Integer.parseInt(req.getParameter("configId"));
            DomainPingConfiguration dpc = DomainPingConfiguration.getById(reping);
            if (dpc.getTarget() != d) {
                return;
            }
            System.out.println("Would now reping: " + dpc.getInfo());
        }
        if (req.getParameter("adddomain") != null) {
            DomainAddForm f = Form.getForm(req, DomainAddForm.class);
            if (f.submit(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        } else if (req.getParameter("domdel") != null) {
            DomainManagementForm f = Form.getForm(req, DomainManagementForm.class);
            if (f.submit(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        }
        super.doPost(req, resp);
    }
}
