package org.cacert.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
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
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        o = req.getParameter("O");
        c = req.getParameter("C");
        st = req.getParameter("ST");
        l = req.getParameter("L");
        email = req.getParameter("contact");
        if (result != null) {
            result.update(o, c, st, l);
            return true;
        }
        Organisation ne = new Organisation(o, c, st, l, email, LoginPage.getUser(req));
        result = ne;
        return true;
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
        if (isEdit) {
            vars.put("edit", true);
        }
        t.output(out, l, vars);
    }
}
