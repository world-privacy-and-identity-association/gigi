package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Domain;
import org.cacert.gigi.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;

public class DomainManagementForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainManagementForm.templ"));

    public DomainManagementForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        return false;
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
