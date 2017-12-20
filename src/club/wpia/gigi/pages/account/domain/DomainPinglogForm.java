package club.wpia.gigi.pages.account.domain;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

public class DomainPinglogForm extends Form {

    private static final Template t = new Template(DomainPinglogForm.class.getResource("DomainPinglogForm.templ"));

    Domain target;

    public DomainPinglogForm(HttpServletRequest hsr, Domain target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        CertificateOwner u = LoginPage.getAuthorizationContext(req).getTarget();

        int i = Integer.parseInt(req.getPathInfo().substring(DomainOverview.PATH.length() + 1));
        Domain d = Domain.getById(i);
        if (u.getId() != d.getOwner().getId()) {
            throw new GigiApiException("Error, owner mismatch.");
        }
        int reping = Integer.parseInt(req.getParameter("configId"));
        DomainPingConfiguration dpc = DomainPingConfiguration.getById(reping);
        if (dpc.getTarget() != d) {
            throw new GigiApiException("Error, target mismatch.");
        }
        dpc.requestReping();
        return new RedirectResult(req.getPathInfo());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final DomainPingExecution[] pings;
        try {
            pings = target.getPings();
        } catch (GigiApiException e) {
            e.format(out, l, vars);
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
                vars.put("state", pings[counter].getState().getDBName());
                vars.put("type", pings[counter].getType());
                vars.put("config", pings[counter].getInfo());
                vars.put("date", pings[counter].getDate());
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
