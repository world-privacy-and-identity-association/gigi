package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;

public class FindUserByDomainForm extends Form {

    public static class FindDomainResult extends SuccessMessageResult {

        private final CertificateOwner owner;

        public FindDomainResult(CertificateOwner owner) {
            super(null);
            this.owner = owner;
        }

        public CertificateOwner getOwner() {
            return owner;
        }
    }

    private CertificateOwner res = null;

    private static final Template t = new Template(FindUserByDomainForm.class.getResource("FindUserByDomainForm.templ"));

    public FindUserByDomainForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String request = req.getParameter("domain");
        Domain d = null;
        if (request.matches("#[0-9]+")) {
            try {
                d = Domain.getById(Integer.parseInt(request.substring(1)));
            } catch (IllegalArgumentException e) {
                throw new GigiApiException(SprintfCommand.createSimple("No personal domains found matching the id {0}", request.substring(1)));
            }
        } else {
            d = Domain.searchDomain(request);
        }
        if (d == null) {
            throw new GigiApiException(SprintfCommand.createSimple("No personal domains found matching {0}", request));
        }
        res = d.getOwner();
        if (res instanceof User) {
            return new RedirectResult(SupportUserDetailsPage.PATH + res.getId() + "/");
        } else if (res instanceof Organisation) {
            return new RedirectResult("/support/domain/" + res.getId());
        } else {
            throw new PermamentFormException(new GigiApiException("Unknown owner type."));
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    public CertificateOwner getRes() {
        return res;
    }

}
