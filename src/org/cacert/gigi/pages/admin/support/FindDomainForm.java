package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;

public class FindDomainForm extends Form {

    private CertificateOwner res = null;

    private static Template t;
    static {
        t = new Template(FindDomainForm.class.getResource("FindDomainForm.templ"));
    }

    public FindDomainForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        String request = req.getParameter("domain");
        Domain d = null;
        if (request.matches("#[0-9]+")) {
            try {
                d = Domain.getById(Integer.parseInt(request.substring(1)));
            } catch (IllegalArgumentException e) {
                throw new GigiApiException(SprintfCommand.createSimple("No personal domains found matching the id {0}", request.substring(1)));
            }
        } else {
            d = Domain.searchUserIdByDomain(request);
        }
        if (d == null) {
            throw new GigiApiException(SprintfCommand.createSimple("No personal domains found matching {0}", request));
        }
        res = d.getOwner();
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    public CertificateOwner getRes() {
        return res;
    }

}
