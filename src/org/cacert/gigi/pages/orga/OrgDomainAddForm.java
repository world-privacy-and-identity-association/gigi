package org.cacert.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class OrgDomainAddForm extends Form {

    public static Template t = new Template(OrgDomainAddForm.class.getResource("OrgDomainAddForm.templ"));

    CertificateOwner target;

    public OrgDomainAddForm(HttpServletRequest hsr, Organisation target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        String domain = req.getParameter("domain");
        new Domain(LoginPage.getUser(req), target, domain);
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }
}