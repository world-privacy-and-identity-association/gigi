package club.wpia.gigi.pages.admin.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.SupportedUser;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;

public class SupportRevokeCertificatesForm extends Form {

    private static final Template t = new Template(SupportRevokeCertificatesForm.class.getResource("SupportRevokeCertificatesForm.templ"));

    private SupportedUser user;

    public SupportRevokeCertificatesForm(HttpServletRequest hsr, SupportedUser user) {
        super(hsr);
        this.user = user;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (user.getTicket() == null) {
            throw new GigiApiException("No ticket number set.");
        }
        user.revokeAllCertificates();
        return new RedirectResult(req.getPathInfo());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final Certificate[] certs = user.getCertificates(true);
        final CertificateProfile[] profiles = CertificateProfile.getAll();
        vars.put("types", new IterableDataset() {

            int typeIndex = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (typeIndex > profiles.length - 1) {
                    return false;
                }
                int valid = 0;
                int total = 0;
                int revoked = 0;
                long lastExpire = Long.MIN_VALUE;
                for (int i = 0; i < certs.length; i++) {
                    try {
                        if (certs[i].getProfile().getId() != profiles[typeIndex].getId()) {
                            continue;
                        }
                        total++;
                        if (certs[i].getStatus() == CertificateStatus.DRAFT) {
                            continue;
                        }
                        if (certs[i].getStatus() == CertificateStatus.REVOKED) {
                            revoked++;
                            continue;
                        }
                        certs[i].cert().checkValidity();
                        lastExpire = Math.max(lastExpire, certs[i].cert().getNotAfter().getTime());
                        valid++;
                    } catch (GeneralSecurityException | IOException | GigiApiException e) {
                        continue;
                    }
                }
                vars.put("total", total);
                vars.put("profile", profiles[typeIndex].getVisibleName());
                vars.put("valid", valid);
                vars.put("exp", total - valid);
                vars.put("rev", revoked);
                if (lastExpire == Long.MIN_VALUE) {
                    vars.put("lastdate", "-");
                } else {
                    vars.put("lastdate", new Date(lastExpire));
                }
                typeIndex++;
                return true;
            }
        });
        t.output(out, l, vars);
    }

}
