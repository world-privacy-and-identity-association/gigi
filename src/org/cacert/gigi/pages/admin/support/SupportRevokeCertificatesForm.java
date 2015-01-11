package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;

public class SupportRevokeCertificatesForm extends Form {

    private static Template t;

    private User user;
    static {
        t = new Template(SupportRevokeCertificatesForm.class.getResource("SupportRevokeCertificatesForm.templ"));
    }

    public SupportRevokeCertificatesForm(HttpServletRequest hsr, User user) {
        super(hsr);
        this.user = user;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        return false;
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
                long lastExpire = Long.MIN_VALUE;
                for (int i = 0; i < certs.length; i++) {
                    try {
                        if (certs[i].getProfile().getId() != profiles[typeIndex].getId()) {
                            continue;
                        }
                        total++;
                        certs[i].cert().checkValidity();
                        lastExpire = Math.max(lastExpire, certs[i].cert().getNotAfter().getTime());
                        valid++;
                    } catch (GeneralSecurityException | IOException e) {
                        continue;
                    }
                }
                vars.put("total", total);
                vars.put("profile", profiles[typeIndex].getVisibleName());
                vars.put("valid", valid);
                vars.put("exp", total - valid);
                vars.put("rev", "TODO");
                if (lastExpire == Long.MIN_VALUE) {
                    vars.put("lastdate", "-");
                } else {
                    vars.put("lastdate", DateSelector.getDateFormat().format(new Date(lastExpire)));
                }
                typeIndex++;
                return true;
            }
        });
        t.output(out, l, vars);
    }

}