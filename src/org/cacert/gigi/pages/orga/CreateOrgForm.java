package org.cacert.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.CountrySelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class CreateOrgForm extends Form {

    private final static Template t = new Template(CreateOrgForm.class.getResource("CreateOrgForm.templ"));

    private Organisation result;

    private String o = "";

    private String st = "";

    private String l = "";

    private String email = "";

    private String optionalName = "";

    private String postalAddress = "";

    private boolean isEdit = false;

    private CountrySelector cs;

    public CreateOrgForm(HttpServletRequest hsr) {
        super(hsr);
        cs = new CountrySelector("C", false);
    }

    public CreateOrgForm(HttpServletRequest hsr, Organisation t) {
        this(hsr);
        isEdit = true;
        result = t;
        o = t.getName();

        cs = new CountrySelector("C", false, t.getState());

        st = t.getProvince();
        l = t.getCity();
        email = t.getContactEmail();
        optionalName = t.getOptionalName();
        postalAddress = t.getPostalAddress();
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String action = req.getParameter("action");
        if (action == null) {
            throw new GigiApiException("No action given.");
        }

        if (action.equals("new")) {
            checkCertData(req);
            checkOrganisationData(req);
            Organisation ne = new Organisation(o, cs.getCountry(), st, l, email, optionalName, postalAddress, LoginPage.getUser(req));
            result = ne;
        } else if (action.equals("updateOrganisationData")) {
            checkOrganisationData(req);
            result.updateOrgData(email, optionalName, postalAddress);
        } else if (action.equals("updateCertificateData")) {
            checkCertData(req);
            result.updateCertData(o, cs.getCountry(), st, l);
        } else {
            throw new GigiApiException("No valid action given.");
        }
        return new RedirectResult(ViewOrgPage.DEFAULT_PATH + "/" + result.getId());
    }

    private void checkOrganisationData(HttpServletRequest req) throws GigiApiException {
        email = extractParam(req, "contact");
        optionalName = extractParam(req, "optionalName");
        postalAddress = extractParam(req, "postalAddress");
        if ( !EmailProvider.isValidMailAddress(email)) {
            throw new GigiApiException("Contact email is not a valid email address");
        }
    }

    private void checkCertData(HttpServletRequest req) throws GigiApiException {
        o = extractParam(req, "O");
        st = extractParam(req, "ST");
        l = extractParam(req, "L");

        if (o.length() > 64 || o.length() < 1) {
            throw new GigiApiException(SprintfCommand.createSimple("{0} not given or longer than {1} characters", "Organisation name", 64));
        }

        cs.update(req);

        if (st.length() > 128 || st.length() < 1) {
            throw new GigiApiException(SprintfCommand.createSimple("{0} not given or longer than {1} characters", "State/county", 128));
        }

        if (l.length() > 128 || l.length() < 1) {
            throw new GigiApiException(SprintfCommand.createSimple("{0} not given or longer than {1} characters", "Town/suburb", 128));
        }
    }

    private String extractParam(HttpServletRequest req, String name) {
        String parameter = req.getParameter(name);
        if (parameter == null) {
            return "";
        }
        return parameter.trim();
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("O", o);
        vars.put("C", cs);
        vars.put("ST", st);
        vars.put("L", this.l);
        vars.put("email", email);
        vars.put("optionalName", optionalName);
        vars.put("postalAddress", postalAddress);
        vars.put("countryCode", cs);
        if (isEdit) {
            vars.put("edit", true);
        }
        t.output(out, l, vars);
    }
}
