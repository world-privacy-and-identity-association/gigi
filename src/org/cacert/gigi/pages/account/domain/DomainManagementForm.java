package org.cacert.gigi.pages.account.domain;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;

public class DomainManagementForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainManagementForm.templ"));

    private CertificateOwner target;

    private boolean foreign;

    public DomainManagementForm(HttpServletRequest hsr, CertificateOwner target, boolean foreign) {
        super(hsr);
        this.target = target;
        this.foreign = foreign;
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        String dels = req.getParameter("delete");

        int delId = Integer.parseInt(dels);
        Domain d = Domain.getById(delId);
        if (d != null && d.getOwner() == target) {
            d.delete();
        } else {
            throw new GigiApiException("Domain was not found.");
        }
        return true;
    }

    public CertificateOwner getTarget() {
        return target;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final Domain[] doms = target.getDomains();
        IterableDataset dts = new IterableDataset() {

            private int point = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (point >= doms.length) {
                    return false;
                }
                Domain domain = doms[point];
                vars.put("id", domain.getId());
                if ( !foreign) {
                    vars.put("domainhref", DomainOverview.PATH + domain.getId());
                }
                vars.put("domain", domain.getSuffix());
                vars.put("status", l.getTranslation(domain.isVerified() ? "verified" : "not verified"));
                point++;
                return true;
            }
        };
        vars.put("domains", dts);
        t.output(out, l, vars);
    }
}
