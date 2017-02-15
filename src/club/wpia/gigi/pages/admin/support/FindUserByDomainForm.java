package club.wpia.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.output.template.Template;

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
