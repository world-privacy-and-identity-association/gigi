package club.wpia.gigi.pages.account.certs;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.CertificateIterable;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

public class CertificateModificationForm extends Form {

    private CertificateOwner target;

    private final boolean withRevoked;

    public CertificateModificationForm(HttpServletRequest hsr, boolean withRevoked) {
        super(hsr);
        this.withRevoked = withRevoked;
        target = LoginPage.getAuthorizationContext(hsr).getTarget();
    }

    private static final Template certTable = new Template(CertificateIterable.class.getResource("CertificateTable.templ"));

    private static final Template myTemplate = new Template(CertificateModificationForm.class.getResource("CertificateModificationForm.templ"));

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String action = req.getParameter("action");
        if ( !"revoke".equals(action)) {
            throw new GigiApiException("Incorrect action given.");
        }
        String[] certs = req.getParameterValues("certs[]");
        if (certs == null) {
            throw new GigiApiException("No certificates to revoke.");
        }
        LinkedList<Job> revokes = new LinkedList<Job>();
        for (String serial : certs) {
            Certificate c = Certificate.getBySerial(serial);
            if (c == null || c.getOwner() != target) {
                continue;
            }
            revokes.add(c.revoke());
        }
        long start = System.currentTimeMillis();
        for (Job job : revokes) {
            int toWait = (int) (60000 + start - System.currentTimeMillis());
            if (toWait > 0) {
                job.waitFor(toWait);
            } else {
                break; // canceled... waited too log
            }
        }
        return new RedirectResult(req.getPathInfo());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("certs", new CertificateIterable(target.getCertificates(withRevoked)));
        vars.put("certTable", certTable);
        if (withRevoked) {
            vars.put("all", "btn-primary");
            vars.put("current", "btn-info");
        } else {
            vars.put("all", "btn-info");
            vars.put("current", "btn-primary");
        }
        myTemplate.output(out, l, vars);
    }

}
