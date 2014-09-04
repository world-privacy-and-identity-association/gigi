package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class DomainOverview extends Page {

    public static final String PATH = "/account/domains/";

    private Template domainDetails = new Template(DomainOverview.class.getResource("DomainDetails.templ"));

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
            try {
                final String[][] pings = d.getPings();
                HashMap<String, Object> vars = new HashMap<>();
                vars.put("domainname", d.getSuffix());
                vars.put("pings", new IterableDataset() {

                    int counter = 0;

                    @Override
                    public boolean next(Language l, Map<String, Object> vars) {
                        if (counter >= pings.length) {
                            return false;
                        }
                        vars.put("state", pings[counter][0]);
                        vars.put("type", pings[counter][1]);
                        vars.put("config", pings[counter][2]);
                        String ping3 = pings[counter][3];
                        if (ping3 == null) {
                            vars.put("result", "");
                        } else {
                            vars.put("result", ping3);
                        }
                        counter++;
                        return true;
                    }
                });
                domainDetails.output(resp.getWriter(), getLanguage(req), vars);
                return;
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), getLanguage(req));
            }

        }
        DomainManagementForm domMan = new DomainManagementForm(req, u);
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
