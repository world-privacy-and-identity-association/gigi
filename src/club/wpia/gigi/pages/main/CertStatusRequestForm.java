package club.wpia.gigi.pages.main;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.util.RateLimit;
import club.wpia.gigi.util.RateLimit.RateLimitException;

public class CertStatusRequestForm extends Form {

    private static final Template t = new Template(CertStatusRequestForm.class.getResource("CertStatusForm.templ"));

    // 50 per 5 min
    public static final RateLimit RATE_LIMIT = new RateLimit(50, 5 * 60 * 1000);

    public static final TranslateCommand NOT_FOUND = new TranslateCommand("Certificate to check not found. Maybe it was issued by a different CA.");

    public CertStatusRequestForm(HttpServletRequest hsr) {
        super(hsr);

    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
            throw new RateLimitException();
        }
        Certificate c = null;
        try {
            c = Certificate.locateCertificate(req.getParameter("serial"), req.getParameter("cert"));
            if (c == null) {
                throw new GigiApiException(NOT_FOUND);
            }
        } catch (GigiApiException e) {
            throw new PermamentFormException(e);
        }

        if (c.getStatus() == CertificateStatus.REVOKED) {
            java.util.Date revocationDate = c.getRevocationDate();
            throw new PermamentFormException(new GigiApiException(SprintfCommand.createSimple("Certificate has been revoked on {0}.", revocationDate)));
        }

        return new SuccessMessageResult(new TranslateCommand("Certificate is valid."));
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
