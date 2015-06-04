package org.cacert.gigi.output;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public class CertificateIterable implements IterableDataset {

    private Certificate[] certificates;

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
        vars.put("state", l.getTranslation(c.getStatus().toString().toLowerCase()));
        vars.put("CN", c.getDistinguishedName());
        vars.put("serial", c.getSerial());
        vars.put("digest", c.getMessageDigest());
        vars.put("profile", c.getProfile().getVisibleName());
        try {
            CertificateStatus st = c.getStatus();
            vars.put("isNotRevoked", st != CertificateStatus.REVOKED);
            if (st == CertificateStatus.ISSUED || st == CertificateStatus.REVOKED) {
                X509Certificate cert = c.cert();
                vars.put("issued", DateSelector.getDateFormat().format(cert.getNotBefore()));
                vars.put("expire", DateSelector.getDateFormat().format(cert.getNotAfter()));
            } else {
                vars.put("issued", l.getTranslation("N/A"));
                vars.put("expire", l.getTranslation("N/A"));
            }
            if (st == CertificateStatus.REVOKED) {
                vars.put("revoked", DateSelector.getDateFormat().format(c.getRevocationDate()));
            } else {
                vars.put("revoked", "n/a");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return true;
    }
}
