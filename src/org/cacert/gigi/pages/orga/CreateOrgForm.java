package org.cacert.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class CreateOrgForm extends Form {

    private final static Template t = new Template(CreateOrgForm.class.getResource("CreateOrgForm.templ"));

    private Organisation result;

    private String o = "";

    private String c = "";

    private String st = "";

    private String l = "";

    private String email = "";

    private String optionalName = "";

    private String postalAddress = "";

    private boolean isEdit = false;

    public CreateOrgForm(HttpServletRequest hsr) {
        super(hsr);
    }

    public CreateOrgForm(HttpServletRequest hsr, Organisation t) {
        super(hsr);
        isEdit = true;
        result = t;
        o = t.getName();
        c = t.getState();
        st = t.getProvince();
        l = t.getCity();
        email = t.getContactEmail();
        optionalName = t.getOptionalName();
        postalAddress = t.getPostalAddress();
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        String action = req.getParameter("action");
        if (action == null) {
            return false;
        }

        if (action.equals("new")) {
            checkCertData(req);
            checkOrganisationData(req);
            Organisation ne = new Organisation(o, c, st, l, email, optionalName, postalAddress, LoginPage.getUser(req));
            result = ne;
            return true;
        } else if (action.equals("updateOrganisationData")) {
            checkOrganisationData(req);
            result.updateOrgData(email, optionalName, postalAddress);
            return true;
        } else if (action.equals("updateCertificateData")) {
            checkCertData(req);
            result.updateCertData(o, c, st, l);
            return true;
        }

        return false;
    }

    private void checkOrganisationData(HttpServletRequest req) throws GigiApiException {
        email = extractParam(req, "contact");
        optionalName = extractParam(req, "optionalName");
        postalAddress = extractParam(req, "postalAddress");
        if ( !EmailProvider.MAIL.matcher(email).matches()) {
            throw new GigiApiException("Contact email is not a valid email address");
        }
    }

    private void checkCertData(HttpServletRequest req) throws GigiApiException {
        o = extractParam(req, "O");
        c = extractParam(req, "C");
        st = extractParam(req, "ST");
        l = extractParam(req, "L");

        if (o.length() > 64 || o.length() < 1) {
            throw new GigiApiException(SprintfCommand.createSimple("{0} not given or longer than {1} characters", "Organisation name", 64));
        }
        if (c.length() != 2) {
            throw new GigiApiException(SprintfCommand.createSimple("{0} not given or not exactly {1} characters long", "Country code", 2));
        }

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

    public Organisation getResult() {
        return result;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("O", o);
        vars.put("C", c);
        vars.put("ST", st);
        vars.put("L", this.l);
        vars.put("email", email);
        vars.put("optionalName", optionalName);
        vars.put("postalAddress", postalAddress);
        if (isEdit) {
            vars.put("edit", true);
        }
        t.output(out, l, vars);
    }
}
