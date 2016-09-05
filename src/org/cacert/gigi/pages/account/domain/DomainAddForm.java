package org.cacert.gigi.pages.account.domain;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.Template;

public class DomainAddForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainAddForm.templ"));

    private User target;

    PingConfigForm pcf;

    public DomainAddForm(HttpServletRequest hsr, User target) throws GigiApiException {
        super(hsr);
        this.target = target;
        pcf = new PingConfigForm(hsr, null);
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        try {
            String parameter = req.getParameter("newdomain");
            if (parameter.trim().isEmpty()) {
                throw new GigiApiException("No domain inserted.");
            }
            Domain d = new Domain(target, target, parameter);
            pcf.setTarget(d);
            pcf.submit(req);
            return true;
        } catch (NumberFormatException e) {
            throw new GigiApiException("A number could not be parsed");
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("pingconfig", new Outputable() {

            @Override
            public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                pcf.outputEmbeddableContent(out, l, vars);
            }
        });
        t.output(out, l, vars);
    }
}
