package club.wpia.gigi.output;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.util.CalendarUtil;

public class CertificateIterable implements IterableDataset {

    private Certificate[] certificates;

    public static final int EXPIRING_IN_DAYS = 14;

    public CertificateIterable(Certificate[] certificates) {
        this.certificates = certificates;
    }

    private int i = 0;

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (i >= certificates.length) {
            return false;
        }
        Certificate c = certificates[i++];
        vars.put("state", c.getStatus());
        vars.put("CN", c.getDistinguishedName());
        vars.put("serial", c.getSerial());
        vars.put("digest", c.getMessageDigest());
        vars.put("profile", c.getProfile().getVisibleName());
        try {
            CertificateStatus st = c.getStatus();
            vars.put("revokable", st != CertificateStatus.REVOKED && st == CertificateStatus.ISSUED);
            String issuedWarning = "";
            String expiredWarning = "";
            if (st == CertificateStatus.ISSUED || st == CertificateStatus.REVOKED) {
                X509Certificate cert = c.cert();
                vars.put("issued", cert.getNotBefore());
                vars.put("expire", cert.getNotAfter());

                if (cert.getNotBefore().after(new Date())) {
                    issuedWarning = "bg-warning";
                }
                vars.put("classIssued", issuedWarning);

                if (cert.getNotAfter().before(CalendarUtil.timeDifferenceDays(EXPIRING_IN_DAYS))) {
                    expiredWarning = "bg-warning";
                }
                if (cert.getNotAfter().before(new Date())) {
                    expiredWarning = "bg-danger";
                }
                vars.put("classExpired", expiredWarning);
            } else {
                vars.put("issued", l.getTranslation("N/A"));
                vars.put("expire", l.getTranslation("N/A"));
            }

            if (st == CertificateStatus.REVOKED) {
                vars.put("revoked", c.getRevocationDate());
            } else {
                vars.put("revoked", l.getTranslation("N/A"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        vars.put("login", c.isLoginEnabled());
        return true;
    }
}
