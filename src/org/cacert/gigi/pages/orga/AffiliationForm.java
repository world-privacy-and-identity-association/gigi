package org.cacert.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.Organisation.Affiliation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class AffiliationForm extends Form {

    Organisation o;

    private static final Template t = new Template(AffiliationForm.class.getResource("AffiliationForm.templ"));

    public AffiliationForm(HttpServletRequest hsr, Organisation o) {
        super(hsr);
        this.o = o;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("del") != null) {
            User toRemove = User.getByEmail(req.getParameter("del"));
            if (toRemove != null) {
                o.removeAdmin(toRemove, LoginPage.getUser(req));
            }
        }
        if (req.getParameter("do_affiliate") != null) {
            User byEmail = User.getByEmail(req.getParameter("email"));
            if (byEmail != null) {
                o.addAdmin(byEmail, LoginPage.getUser(req), req.getParameter("master") != null);
            }
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final List<Affiliation> admins = o.getAllAdmins();
        vars.put("admins", new IterableDataset() {

            Iterator<Affiliation> iter = admins.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !iter.hasNext())
                    return false;
                Affiliation aff = iter.next();
                vars.put("name", aff.getTarget().getName());
                vars.put("master", aff.isMaster() ? l.getTranslation("master") : "");
                vars.put("e-mail", aff.getTarget().getEmail());
                return true;
            }
        });
        t.output(out, l, vars);
    }

    public Organisation getOrganisation() {
        return o;
    }
}
