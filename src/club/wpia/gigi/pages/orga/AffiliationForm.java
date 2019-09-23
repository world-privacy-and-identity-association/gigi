package club.wpia.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.Organisation.Affiliation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

public class AffiliationForm extends Form {

    Organisation o;

    private static final Template t = new Template(AffiliationForm.class.getResource("AffiliationForm.templ"));

    public AffiliationForm(HttpServletRequest hsr, Organisation o) {
        super(hsr);
        this.o = o;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("del") != null) {
            User toRemove = User.getByEmail(req.getParameter("del"));
            if (toRemove != null) {
                o.removeAdmin(toRemove, LoginPage.getUser(req));
                return new RedirectResult(ViewOrgPage.DEFAULT_PATH + "/" + o.getId());
            }
        } else if (req.getParameter("do_affiliate") != null) {
            User byEmail = User.getByEmail(req.getParameter("email"));
            if (byEmail == null) {
                throw new GigiApiException("To add an admin, the email address needs to be known to the system.");
            }
            if (byEmail.canVerify()) {
                o.addAdmin(byEmail, LoginPage.getUser(req), req.getParameter("master") != null);
                return new RedirectResult(ViewOrgPage.DEFAULT_PATH + "/" + o.getId());
            } else {
                throw new GigiApiException("Requested user is not a RA Agent. We need a RA Agent here.");
            }
        }
        throw new GigiApiException("No action could have been carried out.");
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final List<Affiliation> admins = o.getAllAdmins();
        vars.put("admins", new IterableDataset() {

            Iterator<Affiliation> iter = admins.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !iter.hasNext()) {
                    return false;
                }
                Affiliation aff = iter.next();
                Name n = aff.getTarget().getPreferredName();
                vars.put("name", n);
                vars.put("nameString", n.toString());
                vars.put("master", aff.isMaster() ? l.getTranslation("Master") : "");
                vars.put("e-mail", aff.getTarget().getEmail());
                return true;
            }
        });
        t.output(out, l, vars);
    }
}
