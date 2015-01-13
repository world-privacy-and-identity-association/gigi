package org.cacert.gigi.output;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.cacert.gigi.dbObjects.Certificate;
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
        X509Certificate cert;
        try {
            cert = c.cert();
            vars.put("issued", DateSelector.getDateFormat().format(cert.getNotBefore()));
            vars.put("expire", DateSelector.getDateFormat().format(cert.getNotAfter()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        vars.put("revoked", "TODO");// TODO output date
        return true;
    }
}
