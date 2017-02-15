package club.wpia.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.output.template.Template;

public class FindCertForm extends Form {

    public static class FindResult extends SuccessMessageResult {

        private final Certificate[] certs;

        public FindResult(Certificate[] certs) {
            super(null);
            this.certs = certs;
        }

        public Certificate[] getCerts() {
            return certs;
        }
    }

    private static final Template t = new Template(FindCertForm.class.getResource("FindCertForm.templ"));

    private final String SERIAL = "serial";

    private String certType = SERIAL;

    public Certificate certs[];

    public FindCertForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        this.certType = req.getParameter("certType");
        String request = req.getParameter("cert").trim();

        if ( !SERIAL.equals(certType) && !SANType.EMAIL.getOpensslName().equals(certType) && !SANType.DNS.getOpensslName().equals(certType)) {
            throw new GigiApiException("Invalid search type.");
        }

        if (SERIAL.equals(certType)) {
            certs = Certificate.findBySerialPattern(request);
            if (certs.length <= 0) {
                throw new GigiApiException(SprintfCommand.createSimple("No certificate found matching serial number {0}", request));
            }
        }

        if (SANType.EMAIL.getOpensslName().equals(certType) || SANType.DNS.getOpensslName().equals(certType)) {
            SANType stype = SANType.valueOf(certType.toUpperCase());
            certs = Certificate.findBySANPattern(request, stype);
            if (certs.length <= 0) {
                throw new GigiApiException(SprintfCommand.createSimple("No certificate found matching {0}", request));
            }
        }
        return new FindCertForm.FindResult(getCerts());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("serial", !SERIAL.equals(certType) ? "" : "checked");
        vars.put("email", !SANType.EMAIL.getOpensslName().equals(certType) ? "" : "checked");
        vars.put("dns", !SANType.DNS.getOpensslName().equals(certType) ? "" : "checked");

        t.output(out, l, vars);
    }

    public Certificate[] getCerts() {
        return certs;
    }

}
