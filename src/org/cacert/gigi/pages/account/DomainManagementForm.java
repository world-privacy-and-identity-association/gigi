package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Domain;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class DomainManagementForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainManagementForm.templ"));

    private User target;

    public DomainManagementForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            String[] dels = req.getParameterValues("delid[]");
            Domain[] usDomains = target.getDomains();
            for (int i = 0; i < dels.length; i++) {
                int delId = Integer.parseInt(dels[i]);
                for (int j = 0; j < usDomains.length; j++) {
                    if (usDomains[j].getId() == delId) {
                        usDomains[j].delete();
                        break;
                    }
                }
            }
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final Domain[] doms = (Domain[]) vars.get("doms");
        IterableDataset dts = new IterableDataset() {

            private int point = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (point >= doms.length) {
                    return false;
                }
                Domain domain = doms[point];
                vars.put("id", domain.getId());
                vars.put("domain", domain.getSuffix());
                vars.put("status", "??");
                point++;
                return true;
            }
        };
        vars.put("domains", dts);
        t.output(out, l, vars);
    }

}
