package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Domain;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class DomainAddForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainAddForm.templ"));

    private User target;

    public DomainAddForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
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
            return true;
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
