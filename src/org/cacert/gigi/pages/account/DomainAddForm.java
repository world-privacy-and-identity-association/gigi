package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class DomainAddForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainAddForm.templ"));

    private User target;

    PingconfigForm pcf;

    public DomainAddForm(HttpServletRequest hsr, User target) throws GigiApiException {
        super(hsr);
        this.target = target;
        pcf = new PingconfigForm(hsr, null);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            String parameter = req.getParameter("newdomain");
            if (parameter.trim().isEmpty()) {
                throw new GigiApiException("No domain inserted.");
            }
            Domain d = new Domain(target, parameter);
            d.insert();
            pcf.setTarget(d);
            pcf.submit(out, req);
            return true;
        } catch (NumberFormatException e) {
            new GigiApiException("A number could not be parsed").format(out, Page.getLanguage(req));
            return false;
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
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
