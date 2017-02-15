package club.wpia.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

public class OrgDomainAddForm extends Form {

    private static final Template t = new Template(OrgDomainAddForm.class.getResource("OrgDomainAddForm.templ"));

    Organisation target;

    public OrgDomainAddForm(HttpServletRequest hsr, Organisation target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String domain = req.getParameter("domain");
        new Domain(LoginPage.getUser(req), target, domain);
        return new RedirectResult(ViewOrgPage.DEFAULT_PATH + "/" + target.getId());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }
}
