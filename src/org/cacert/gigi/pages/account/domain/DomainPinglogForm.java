package org.cacert.gigi.pages.account.domain;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.DomainPingExecution;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class DomainPinglogForm extends Form {

    static Template t = new Template(DomainPinglogForm.class.getResource("DomainPinglogForm.templ"));

    Domain target;

    public DomainPinglogForm(HttpServletRequest hsr, Domain target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        User u = Page.getUser(req);

        int i = Integer.parseInt(req.getPathInfo().substring(DomainOverview.PATH.length()));
        Domain d = Domain.getById(i);
        if (u.getId() != d.getOwner().getId()) {
            return false;
        }
        int reping = Integer.parseInt(req.getParameter("configId"));
        DomainPingConfiguration dpc = DomainPingConfiguration.getById(reping);
        if (dpc.getTarget() != d) {
            return false;
        }
        dpc.requestReping();
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final DomainPingExecution[] pings;
        try {
            pings = target.getPings();
        } catch (GigiApiException e) {
            e.format(out, l);
            return;
        }
        vars.put("domainname", target.getSuffix());
        vars.put("pings", new IterableDataset() {

            int counter = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (counter >= pings.length) {
                    return false;
                }
                vars.put("state", pings[counter].getState());
                vars.put("type", pings[counter].getType());
                vars.put("config", pings[counter].getInfo());
                String ping3 = pings[counter].getResult();
                if (ping3 == null) {
                    vars.put("result", "");
                } else {
                    vars.put("result", ping3);
                }
                DomainPingConfiguration dpc = pings[counter].getConfig();
                if (dpc != null) {
                    vars.put("configId", Integer.toString(dpc.getId()));
                }
                counter++;
                return true;
            }
        });
        t.output(out, l, vars);
    }
}
