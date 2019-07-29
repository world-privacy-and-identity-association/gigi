package club.wpia.gigi.pages.account.certs;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.SupportedUser;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;

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
    public RedirectResult submit(HttpServletRequest req) throws GigiApiException {
        if (target != null) {
            target.revokeCertificate(c);
        } else {
            c.revoke(RevocationType.USER).waitFor(Job.WAIT_MIN);
        }
        return new RedirectResult(req.getPathInfo());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
