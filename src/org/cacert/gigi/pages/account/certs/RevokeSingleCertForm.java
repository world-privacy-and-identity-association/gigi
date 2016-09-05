package org.cacert.gigi.pages.account.certs;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class RevokeSingleCertForm extends Form {

    private static final Template t = new Template(RevokeSingleCertForm.class.getResource("RevokeSingleCertForm.templ"));

    private Certificate c;

    private SupportedUser target;

    public RevokeSingleCertForm(HttpServletRequest hsr, Certificate c, SupportedUser target) {
        super(hsr);
        this.c = c;
        this.target = target;
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        if (target != null) {
            target.revokeCertificate(c);
        } else {
            c.revoke().waitFor(60000);
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
