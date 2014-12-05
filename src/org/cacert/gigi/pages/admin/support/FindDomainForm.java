package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class FindDomainForm extends Form {

    private int userId = -1;

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
        if (request.matches("#[0-9]+")) {
            try {
                Domain domainById = Domain.getById(Integer.parseInt(request.substring(1)));
                userId = domainById.getOwner().getId();
            } catch (IllegalArgumentException e) {
                throw (new GigiApiException("No personal domains found matching the id " + request.substring(1) + "."));
            }
        } else {
            userId = Domain.searchUserIdByDomain(request);
        }
        if (userId == -1) {
            throw (new GigiApiException("No personal domains found matching " + request));
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    public int getUserId() {
        return userId;
    }

}
